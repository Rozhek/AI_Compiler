package org.l2junity.decompiler.reader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2junity.decompiler.dataholder.ComparisonMapper;
import org.l2junity.decompiler.dataholder.HandlerConstructor;
import org.l2junity.decompiler.dataholder.HandlerMapper;
import org.l2junity.decompiler.dataholder.LibraryMapper;
import org.l2junity.decompiler.dataholder.MapperDataHolder;
import org.l2junity.decompiler.dataholder.ParamsMapper;
import org.l2junity.decompiler.enums.StatementType;
import org.l2junity.decompiler.model.FunctionController;

/**
 * AI class reader
 *
 * @author malyelfik
 */
public final class ClassReader {
    // System line separator
    private static final String EOL = System.lineSeparator();
    private static final Pattern SKILL_PARAM_REGEX = Pattern.compile("_skill(_)?[0-9]{0,}$|^skill(_)?[0-9]+(_id)?$|^specialskill(_)?[0-9]+(_id)?$");
    // Reader data
    private int _indent;
    private int _currLine;
    private final AiReader _reader;
    private final List<String> _lines;
    private Set<String> _constructParams = new HashSet<>();
    private Set<String> _importLibs = new HashSet<>();
    private Set<String> _handlerVars = new HashSet<>();
    private Set<String> _params = new HashSet<>();
    private String _name;

    public ClassReader(AiReader reader, List<String> lines) throws Exception {
        _indent = 0;
        _currLine = 0;
        _lines = lines;
        _reader = reader;
        _name = "";
    }

    public StringBuilder read1() throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append(getLine()).append(EOL);
        try {
            while (readLine() != null) {
                sb.append(getLine()).append(EOL);
            }
        } catch (IndexOutOfBoundsException e) {
            return sb;
        }
        return sb;
    }

    /**
     * Read & save AI class to file
     *
     * @throws Exception
     */
    public StringBuilder read() throws Exception {
        String[] split = getLine().split("\\s");

        final StringBuilder sb = new StringBuilder();
        final String name = split[2];
        sb.append("package ").append(LibraryMapper.getInstance().get("package")).append(";").append(EOL).append(EOL);

        _name = name;
        _importLibs.add("NpcInstance");
        int libPoint = sb.length();

        if (!split[4].equals("(null)"))
            sb.append(EOL).append("public class ").append(name).append(" extends ").append(split[4]).append(EOL);
        else {
            sb.append("import org.mmocore.gameserver.ai.NpcAI").append(";").append(EOL).append(EOL);
            sb.append("public class ").append(name).append(" extends ").append("NpcAI").append(EOL);
        }
        sb.append("{").append(EOL);

        _indent++;
        int constructorPoint = sb.length();
        while (readLine() != null) {
            split = getLine().split("\\s");
            switch (split[0]) {
                case "parameter_define_begin":
                    readParameters(sb, name);
                    constructorPoint = sb.length();
                    break;
                case "property_define_begin":
                    readProperties(sb);
                    constructorPoint = sb.length();
                    break;
                case "handler":
                    readHandler(sb, split[5]);
                    break;
                case "class_end": {
                    sb.insert(constructorPoint, getConstructor(name));
                    sb.append("}");

                    if (_importLibs.isEmpty())
                        return sb;

                    StringBuilder imports = new StringBuilder();
                    String path;
                    for (String lib : _importLibs) {
                        path = LibraryMapper.getInstance().get(lib);
                        if (path != null)
                            imports.append("import ").append(path).append(".").append(lib).append(";").append(EOL);
                    }
                    sb.insert(libPoint, imports.toString());
                    return sb;
                }
            }
        }
        return null;
    }

    private String getConstructor(String name) {
        StringBuilder sb = new StringBuilder();
        if (_constructParams.isEmpty()) {
            append(sb, "public " + name + "(final NpcInstance actor){super(actor);}");
        } else {
            append(sb, "public " + name + "(final NpcInstance actor)");
            append(sb, "{");
            append(sb, "super(actor);");
            for (String param : _constructParams)
                append(sb, param);
            append(sb, "}");
        }
        sb.append(EOL);
        return sb.toString();
    }

    /**
     * Read block which starts with <b>parameter_define_begin</b> and ends with <b>parameter_define_end</b>
     *
     * @param sb buffer where read data should be stored
     */
    private void readParameters(StringBuilder sb, String ai_class) {
        //append(sb, "parameters:");
        //_indent++;
        Set<String> params = new HashSet<>();

        while (readLine() != null) {
            final String line = getLine();
            if (line.startsWith("parameter_define_end")) {
                //_indent--;
                sb.append(EOL);
                return;
            }

            final String[] split = line.split("\\s");
            if (split.length >= 2) {
                String type = split[0];
                if (type.equals("string"))
                    type = "String";
                else if (type.equals("waypointstype"))
                    type = "String";
                else if (type.equals("waypointdelaystype"))
                    type = "int";

                final String name = split[1];
                if (name.equals("SuperPointMethod")) {
                    type = "SuperPointRail";
                } else if (name.contains("Bonus") && type.equals("int")) {
                    type = "float";
                }
                if (!type.isEmpty() && !type.equals("int") && !type.equals("float") && !type.equals("boolean"))
                    _importLibs.add(type);

                String value = (split.length >= 3) ? line.substring(line.indexOf(split[1]) + split[1].length()).trim() : null;
                if (type.equals("float"))
                    value += "f";
                else if (value != null && name.equals("Max_Desire") && value.matches("\\d{8,}"))
                    value = value.substring(0, 8);
                if (value == null) {
                    if (params.contains(name))
                        continue;
                    append(sb, type + " " + name, false);
                    params.add(name);
                    _params.add(name);
                } else {
                    if (params.contains(name))
                        continue;
                    final Matcher skillmatcher = SKILL_PARAM_REGEX.matcher(name.toLowerCase());
                    //if(ParamsMapper.getInstance().hasHolderAndItsNot(name, ai_class)) {
                    if (paramExists(name)) {
                        if (skillmatcher.find())
                            value = MapperDataHolder.getInstance().get("skillDataComment", value, value, _importLibs);
                        _constructParams.add(name + " = " + value + ";");
                        params.add(name);
                        continue;
                    }

                    if (skillmatcher.find()) {
                        value = MapperDataHolder.getInstance().get("skillData", value, value, _importLibs);
                        append(sb, value);
                        append(sb, "public " + type + " " + name, false);
                        _importLibs.add("SkillInfo");
                    } else {
                        value = ComparisonMapper.getInstance().get(name, value, _importLibs);
                        append(sb, "public " + type + " " + name + " = " + value, false);
                    }
                    params.add(name);
                    _params.add(name);
                }
                sb.append(";").append(EOL);
            }
        }
    }

    /**
     * Read block which starts with <b>property_define_begin</b> and ends with <b>property_define_end</b>
     *
     * @param sb buffer where read data should be stored
     */
    private void readProperties(StringBuilder sb) {
        append(sb, "//property:");
        _indent++;

        while (readLine() != null) {
            final String[] split = getLine().split("\\s");
            switch (split[0]) {
                case "telposlist_begin":
                    readTeleport(sb, split[1]);
                    break;
                case "buyselllist_begin":
                    readBuyList(sb, split[1]);
                    break;
                case "property_define_end": {
                    _indent--;
                    sb.append(EOL);
                    return;
                }
            }
        }
    }

    /**
     * Read block which starts with <b>telposlist_begin</b> and ends with <b>telposlist_end</b>
     *
     * @param sb buffer where read data should be stored
     */
    private void readTeleport(StringBuilder sb, String name) {
        append(sb, "//teleportList " + name + ":");
        _indent++;

        while (readLine() != null) {
            if (getLine().startsWith("telposlist_end")) {
                _indent--;
                sb.append(EOL);
                return;
            }
            append(sb, getLine());
        }
    }

    /**
     * Read block which starts with <b>buyselllist_begin</b> and ends with <b>buyselllist_end</b>
     *
     * @param sb buffer where read data should be stored
     */
    private void readBuyList(StringBuilder sb, String name) {
        append(sb, "//buyList " + name + ":");
        _indent++;

        while (readLine() != null) {
            if (getLine().startsWith("buyselllist_end")) {
                _indent--;
                sb.append(EOL);
                return;
            }
            append(sb, getLine());
        }
    }

    /**
     * Read block which starts with <b>handler</b> and ends with <b>handler_end</b>
     *
     * @param sb buffer where read data should be stored
     */
    private void readHandler(StringBuilder sb, String name) {
        String handlerName = HandlerMapper.getInstance().get(name);
        _handlerVars.clear();
        //append(sb, "EventHandler ", true);
        if (handlerName.equals(name))
            append(sb, "/*", true);

        append(sb, "@Override", true);
        append(sb, "protected void " + handlerName + "(", false);
        //int paramPoint = sb.length();
        Set<String> params = HandlerMapper.getInstance().getParams(name);
        int prec = 1;
        for (String param : params) {
            String type = MapperDataHolder.getInstance().get("EventHandlerParam", param, _importLibs);
            sb.append(type).append(" ").append(param);
            if (prec < params.size())
                sb.append(", ");
            prec++;
            switch (type) {
                case "int":
                case "boolean":
                case "float":
                    break;
                default:
                    _importLibs.add(type);
            }
        }
        sb.append(")").append(EOL);
        append(sb, "{");
        readHandlerParams(sb, handlerName, params);
		/*Set<String> params = readHandlerParams(sb);
		if(params!=null) {
			sb.insert(paramPoint, "EventParam params");
			_importLibs.add("EventParam");
			_importLibs.add("CtrlEventParam");
		}*/
        int actorPoint = sb.length();
        sb.append(EOL);

        readHandlerContent(sb, handlerName, params);

        for (String var : _handlerVars) {
            String value = HandlerConstructor.getInstance().get(var);
            if (value != null)
                sb.insert(actorPoint, "\t\t" + value + ";" + EOL);
        }

        append(sb, "}" + EOL);
        if (handlerName.equals(name))
            append(sb, "*/", true);
    }

    /**
     * Read handler parameters.<br>
     * Reads data from <b>variable_begin</b> to <b>variable_end</b>.
     *
     * @param sb buffer where read data should be stored
     */
    private Set<String> readHandlerParams(StringBuilder sb, String handlerName, Set<String> params) {
        Set<String> writted = new HashSet<>();
        readLine();
        if (getLine().startsWith("variable_begin")) {
            //boolean isFirst = true;
            //boolean isFinal;
            Set<String> handlerParams = new HashSet<>();
            while (readLine() != null) {
                if (getLine().startsWith("variable_end")) {
                    return handlerParams;
                }

                String param = getLine().split("\"")[1];
                if (!param.equals("myself") && !param.equals("_choiceN") && !param.equals("_code") && !param.equals("_from_choice")) {
                    param = getParam(param);
                    if (params.contains(param))
                        continue;
                    else if (param.equals("h0"))
                        continue;
                    else if (writted.contains(param))
                        continue;
                    writted.add(param);
                    //isFinal = false;
                    String type = MapperDataHolder.getInstance().get("EventHandlerParam", param, _importLibs);
					/*if(type == null) {
						type = MapperDataHolder.getInstance().get("EventHandlerFinal", param);
						if(type == null)
							_reader.appendToLog("Handler param " + param + " not defined!");
						else
							isFinal = true;
					}*/
                    if (type == null) {
                        _reader.appendToLog("Handler param " + param + " not defined!");
                        sb.append("\t\t").append("Unknown ").append(param).append(";").append(EOL);
                        continue;
                    }
                    String value = null;
                    switch (type) {
                        case "boolean":
                            value = "false";
                            break;
                        case "int":
                        case "float":
                            value = "0";
                            break;
                        default:
                            value = "null";
                            _importLibs.add(type);
                    }
                    if (handlerName.equals("onEvtDead") && param.equals("lparty")) {
                        sb.append("\t\t").append(type).append(" ").append(param).append(" = attacker.getPlayer() != null? attacker.getPlayer().getParty() : null;").append(EOL);
                        continue;
                    } else if (handlerName.equals("onEvtTimerFiredEx") && param.equals("i0")) {
                        sb.append("\t\t").append(type).append(" ").append(param).append(" = 0;").append(EOL);
                        continue;
                    }
                    if (value != null)
                        sb.append("\t\t").append(type).append(" ").append(param).append(" = ").append(value).append(";").append(EOL);
                    else
                        sb.append("\t\t").append(type).append(" ").append(param).append(";").append(EOL);
					/*String value;
					switch(type){
						case "int":
							value = "params.getInt(CtrlEventParam."+ param +")";
							break;
						case "boolean":
							value = "params.getBool(CtrlEventParam."+ param +")";
							break;
						case "float":
							value = "params.getFloat(CtrlEventParam."+ param +")";
							break;
						default:
							value = "params.get(CtrlEventParam."+ param +", " + type + ".class)";
							_importLibs.add(type);
					}
					sb.append("\t\t").append(isFinal? "final " + type : type).append(" ").append(param).append(" = ").append(value).append(";").append(EOL);
					sb.append("\t\t").append(type).append(" ").append(param).append(" = ").append(value).append(";").append(EOL);
					handlerParams.add(param);
					if (isFirst)
					{
						sb.append(isFinal? "final " + type : type).append(" ").append(param);
						handlerParams.append(param);
						isFirst = false;
					}
					else
					{
						sb.append(", ").append(isFinal? "final " + type : type).append(" ").append(param);
						handlerParams.append(", ").append(param);
					}
					*/
                }
            }
        }
        return null;
    }

    /**
     * Read handler content.<br>
     *
     * @param sb buffer where read data should be stored
     */
    private void readHandlerContent(StringBuilder sb, String handlerName, Set<String> paramOverrides) {
        // Read strings holder
        final Map<String, String> strings = new HashMap<>();
        //Set<String> paramOverrides = new HashSet<>();
        // Parameters
        final Stack<String> parameters = new Stack<>();
        boolean skipAdd = false;
        String addVal = "";
        boolean isBoolean = false;
        // Conditions data
        final Stack<String> jumps = new Stack<>();
        final Stack<StatementType> statements = new Stack<>();
        final Stack<String> switchData = new Stack<>();
        String lastJump = null;

        while (readLine() != null) {
            final String[] split = getLine().split("\\s");
            if (isBoolean)
                if (!split[0].equals("push_const") && !split[0].equals("shift_sp") && !split[0].equals("equal") && !split[0].equals("not_equal") && !split[0].equals("add") && !split[0].equals("fetch_i") && !split[0].equals("fetch_i4"))
                    isBoolean = false;
            switch (split[0]) {
                case "push_const": // Push constant to stack
                {
                    final String prevLine = getLine(-1);
                    if (prevLine.startsWith("push_event")) {
                        skipAdd = true;
                        String var = prevLine.split("//")[1].trim();
                        var = getParam(var);
                        String type = MapperDataHolder.getInstance().get("EventHandlerParam", var, _importLibs);
                        if (type != null && type.equals("boolean")) isBoolean = true;
                        if (var.equals("c0") && getLine(3).contains("branch_false"))
                            var += " !=null";
                        else if (var.equals("i2") && getLine(3).contains("branch_false"))
                            var += " !=0";
                        parameters.push(var); // Push event name
                    } else {
                        String name = getLine().split("//")[1].trim();
                        if (name.startsWith("unary") || !getLine(1).startsWith("add")) // Constant
                        {
                            String var = split[1];
                            var = getParam(var);
                            if (name.startsWith("unary") && var.matches("\\d{1,10}\\.\\d{1,6}"))
                                var = var + "f";
                            else if (name.startsWith("unary") && var.matches("\\d{10,}") && Double.parseDouble(var) > Integer.MAX_VALUE)
                                var = var + "d";
                            String type = MapperDataHolder.getInstance().get("EventHandlerParam", var, _importLibs);
                            if (type != null && type.equals("boolean")) isBoolean = true;
                            parameters.push(var);
                        } else // Variable
                        {
                            skipAdd = true;
                            String var = parameters.pop();
                            name = getVarWithCaller(var, name);
                            var = getVar(var);
                            if (name == null) {
                                parameters.push(var);
                                break;
                            }
                            String type = MapperDataHolder.getInstance().get("EventHandlerParam", name, _importLibs);
                            if (type != null && type.equals("boolean")) isBoolean = true;
                            if (name.equals("getPlayer()") && getLine(3).contains("branch_false"))
                                name += " !=null";
                            else if (name.equals("i_quest9") && getLine(3).contains("branch_false"))
                                name += " !=0";
                            else if (var != null && var.equals("actor") && name.equals("p_state"))
                                var = null;
                            parameters.push(var == null || name.equals("++") ? name : var + "." + name);
                        }
                    }
                    break;
                }
                case "push_parameter": // Push parameter to stack
                case "push_property": // Push property to stack
                    String var = split[1];
                    if (var.equals("WeaponID") && getLine(3).contains("branch_false"))
                        var += " !=0";
                    else if (var.equals("babble_mode") && getLine(1).contains("branch_false"))
                        var += " !=0";
                    parameters.push(var);
                    break;
                case "push_string": // Push string to stack
                    parameters.push(strings.get(split[1]));
                    break;
                case "add": // Merge two values in stack together
                {
                    if (!skipAdd) {
                        pushArithmetic(parameters, "+", true);
                    } else {
                        skipAdd = false;
                    }
                    break;
                }
                case "func_call": // Build function using parameters from stack
                {
                    // Read func name and set default values for parameters count and type
                    final String name = split[4].trim().substring(5, split[4].length() - 1);
                    final FunctionController holder = new FunctionController(name, _importLibs);
                    isBoolean = name.startsWith("Is") || name.startsWith("InMy") || name.startsWith("CanAttack");
                    if (name.equals("IsInCategory"))
                        _importLibs.add("Category");
                    else if (name.startsWith("BroadcastScriptEvent") || name.startsWith("SendScriptEvent"))
                        _importLibs.add("ScriptEvent");
                    else if (name.startsWith("AddEffectActionDesire"))
                        _importLibs.add("SocialAction");
                    boolean isProcedure = false;
                    int paramCount = 0;

                    // Read parameters count ant func type
                    for (int i = 1; i <= 2; i++) {
                        String line = getLine(i);
                        if (!line.contains("shift_sp")) {
                            break;
                        }

                        final String param = line.split("\\s")[1].substring(1);
                        if (i == 1) {
                            paramCount = Integer.parseInt(param);
                        } else {
                            isProcedure = param.equals("1");
                        }
                    }

                    //if (name.equals("HaveMemo")  && getLine(1).contains("shift_sp") && getLine(2).contains("push_const"))
                    //	isBoolean = true;
                    if (name.equals("HaveMemo") && getLine(1).contains("shift_sp") && (getLine(2).contains("and") || getLine(2).contains("or") || getLine(2).contains("push_reg_sp") || getLine(2).contains("branch_false") || getLine(2).startsWith("L")))
                        holder.addValue("!=0");
                    else if (name.equals("OwnItemCount") && getLine(1).contains("shift_sp") && (getLine(2).contains("and") || getLine(2).contains("or") || getLine(2).contains("push_reg_sp") || getLine(2).contains("branch_false") || getLine(2).startsWith("L")))
                        holder.addValue("!=0");
                    // Check if function has caller and calculate count of required parameters
                    final boolean hasCaller = (!isProcedure || (paramCount != 0));
                    final int requiredParams = paramCount + ((hasCaller) ? 1 : 0);
                    final int paramSize = parameters.size();

                    // Build string form of function (because parameters are stored in stack we must build it from end)
                    if (requiredParams <= paramSize) {
                        for (int i = paramCount; i > 0; i--) {
                            String param = stripBrackets(parameters.pop());
                            param = getParam(param);
                            holder.addParameter(param);
                        }
                    } else {
                        // If required parameters are two but stack contains only one then in stack is name of function caller and function itself has zero parameters
                        if ((paramSize != 1) || (requiredParams != 2)) {
                            _reader.appendToLog("Invalid parameters count for function " + name + ", required " + requiredParams + " has " + parameters.size() + "!");
                        }
                        isProcedure = true;
                    }
                    // If function has caller append him to beginning
                    if (hasCaller) {
                        String caller = getVar(parameters.pop());
                        holder.setCaller(caller);
                    }

                    // If function is procedure then write it to file, otherwise push it to stack
                    final String func = holder.toString();
                    if (isProcedure) {
                        append(sb, func + ";");
                    } else {
                        parameters.push(func);
                    }
                    break;
                }
                case "add_string": // Concatenate strings
                    pushArithmetic(parameters, "+", false);
                    break;
                case "sub": // Subtract
                    pushArithmetic(parameters, "-", true);
                    break;
                case "mul": // Multiple
                    pushArithmetic(parameters, "*", true);
                    break;
                case "div": // Divide
                    pushArithmetic(parameters, "/", true);
                    break;
                case "equal": // Equal
                {
                    if (parameters.size() > 1) {
                        if (isBoolean)
                            pushBComparison(parameters, false);
                        else
                            pushComparison(parameters, "==");
                    }
                    break;
                }
                case "greater": // Greater than
                    addVal = ">" + parameters.peek();
                    pushComparison(parameters, ">");

                    if (_name.equals("ai_boss02_b02_premo") && handlerName.equals("onEvtTimerFiredEx"))
                        handlerName.equals("onEvtTimerFiredEx");
                    break;
                case "greater_equal": // Greater than or equal
                    addVal = ">=" + parameters.peek();
                    pushComparison(parameters, ">=");
                    break;
                case "not_equal": // Not equal
                    if (isBoolean)
                        pushBComparison(parameters, true);
                    else
                        pushComparison(parameters, "!=");
                    break;
                case "less_equal": // Lower than or equal
                    addVal = "<=" + parameters.peek();
                    pushComparison(parameters, "<=");
                    break;
                case "less": // Lower than
                    addVal = "<" + parameters.peek();
                    pushComparison(parameters, "<");
                    break;
                case "and": // Logical and
                    pushLogic(parameters, "&&");
                    break;
                case "or": // Logical or
                    pushLogic(parameters, "||");
                    break;
                case "mod": // Modulo
                    pushArithmetic(parameters, "%", true);
                    break;
                case "bit_and": // Bit and
                    pushArithmetic(parameters, "&", true);
                    break;
                case "bit_or": // Bit or
                    pushArithmetic(parameters, "|", true);
                    break;
                case "not": // Bit inverse
                {
                    final String val = parameters.pop();
                    final String newVal = (val.startsWith("~")) ? val.substring(1) : ("~" + val);
                    parameters.push(newVal);
                    break;
                }
                case "negate": // Number inverse
                {
                    final String val = parameters.pop();
                    final String newVal = (val.startsWith("-")) ? val.substring(1) : ("-" + val);
                    parameters.push(newVal);
                    break;
                }
                case "exit_handler": // Return
                    append(sb, "return;");
                    break;
                case "call_super": // Call superclass
                    String parambuilder = "";
                    //sb.append("\t\t").append("super.").append(handlerName).append("(");
                    int psize = 1;
                    for (String par : paramOverrides) {
                        parambuilder += (psize < paramOverrides.size() ? par + ", " : par);
                        psize++;
                    }
                    //sb.append(");").append(EOL);
                    append(sb, "super." + handlerName + "(" + parambuilder + ");");
                    break;
                case "branch_false": // Condition false branch
                {
                    addVal = "";
                    final String nextLine = getLine(1);
                    if (!statements.isEmpty() && statements.peek().equals(StatementType.CASE)) // Write case
                    {
                        final String switchValue = switchData.peek();
                        final String param = parameters.pop();

                        append(sb, "case " + ComparisonMapper.getInstance().get(switchValue, param, _importLibs) + ":");
                        append(sb, "{");

                        statements.pop();
                        jumps.push(split[1]);
                        break;
                    } else if (isSwitch(split[1])) // Write switch
                    {
                        final String[] params = parameters.pop().split("\\s");
                        append(sb, "switch (" + params[0] + ")");
                        append(sb, "{");
                        append(sb, "case " + params[2] + ":");
                        append(sb, "{");

                        switchData.push(params[0]);
                        statements.push(StatementType.SWITCH);
                        jumps.push(split[1]);
                    } else if (nextLine.contains("jump")) // Prepare for "for" statement
                    {
                        append(sb, parameters.pop());
                        statements.push(StatementType.FOR_START);
                        jumps.push(split[1]);
                    } else if ((lastJump != null) && isWhile(split[1], lastJump)) // Write while
                    {
                        String cond = parameters.pop();
                        if (!(cond.startsWith("(") && cond.endsWith(")") && !cond.endsWith("()"))) {
                            cond = "(" + cond + ")";
                        }
                        append(sb, "while " + cond);
                        append(sb, "{");

                        statements.push(StatementType.ELSE);
                        jumps.push(split[1]);
                    } else if (nextLine.startsWith("L") && !parameters.isEmpty()) // Write if / else if / else
                    {
                        String cond = parameters.pop();
                        if (cond.equals("1") || cond.equals("0")) {
                            cond = cond.equals("1") ? "(true)" : "(false)";
                        } else if (!(cond.startsWith("(") && cond.endsWith(")") && !cond.endsWith("()"))) {
                            cond = "(" + cond + ")";
                        } else if (cond.lastIndexOf("(") > cond.substring(0, cond.length() - 1).lastIndexOf(")")) {
                            cond = "(" + cond + ")";
                        }
                        append(sb, "if " + cond);
                        if (isElseIf(sb)) {
                            jumps.pop();
                            statements.pop();
                        } else {
                            append(sb, "{");
                        }
                        statements.push(StatementType.IF);
                        jumps.push(split[1]);
                    }
                    break;
                }
                case "push_reg_sp": // Clone stack top value (used for incrementing for example)
                {
                    if (getLine(2).contains("push_reg_sp")) {
                        parameters.push(parameters.peek());
                    }
                    break;
                }
                case "assign":
                case "assign4": // Merge two values together with "=" as delimiter and put it back to stack. This operators are also used for "for" statement.
                {
                    // Check if it should be "for"
                    if (!statements.isEmpty() && statements.peek().equals(StatementType.FOR_START)) {
                        // Remove empty line
                        sb.delete(sb.lastIndexOf(EOL), sb.length());

                        // Get last 2 lines content
                        final String[] params = new String[2];
                        for (int i = params.length - 1; i >= 0; i--) {
                            final int pos = sb.lastIndexOf(EOL);
                            params[i] = sb.substring(pos).trim().replaceAll("\\s+$", "");
                            sb.delete(pos, sb.length());
                        }
                        sb.append(EOL);

                        // Write for
                        final String val = parameters.pop();
                        final String inc = parameters.pop() + (val.equals("++") ? val : " = " + val);
                        append(sb, "for (" + params[0] + " " + params[1] + "; " + inc + ")");
                        append(sb, "{");
                        statements.pop();
                        statements.push(StatementType.FOR);
                    } else // Concat values
                    {
                        // Read data from stack
                        final String val1 = parameters.pop();
                        final String val2 = parameters.pop();
                        // Assign data & push back to stack
                        String newVal = val2 + " = " + stripBrackets(val1);
                        if (parameters.isEmpty()) {
                            newVal += ";";
                        }
                        parameters.push(newVal);
                        //if(handlerParams.contains(val2))
                        //	paramOverrides.add(val2);
                    }
                    break;
                }
                case "shift_sp": // Write assign to file
                {
                    if (!parameters.isEmpty() && split[1].equals("-1")) {
                        final String prevLine = getLine(-1);
                        if (!prevLine.contains("shift_sp") && !prevLine.contains("func_call")) {
                            append(sb, parameters.pop());
                        }
                        final String nextLine = getLine(1);
                        if (nextLine.contains("and") && nextLine.contains("or")) {
                            append(sb, parameters.pop());
                        }
                    } else if (!parameters.isEmpty() && !addVal.isEmpty() && split[1].equals("-2")) {
                        //final String prevLine = getLine(-1);
                        final String nextLine = getLine(1);
                        if (nextLine.equals("and") || nextLine.equals("or") && !isBoolean) {
                            parameters.push(parameters.pop() + " " + addVal);
                            addVal = "";
                        } else if (nextLine.equals("push_const"))
                            addVal = "";
						/*else if(prevLine.contains("func_call") && !(nextLine.equals("push_const") || nextLine.equals("push_event")))
							parameters.push(parameters.pop() + " !=0");*/
                    }
                    break;
                }
                case "handler_end": // Notify handler end
                    return;
                case "fetch_i4":
                    final String nextLine = getLine(1);//Check empty call
                    if (nextLine.startsWith("L")) {
                        String val = parameters.pop();
                        val += ";";
                        parameters.push(val);
                    }
                    break;
                default: {
                    if (split[0].startsWith("S")) // Read line starting with S <-> read string to memory
                    {
                        final String line = getLine();
                        final int index = line.indexOf(".");
                        final String key = line.substring(0, index);
                        final String value = line.substring(index + 1).trim();
                        strings.put(key, value);
                    } else if (split[0].startsWith("L")) // Read line starting with L <-> read jump target
                    {
                        if (jumps.contains(split[0])) // Check if jump name is already registered
                        {
                            switch (statements.peek()) // Check which block type is currently processed
                            {
                                case IF: // If block is IF
                                {
                                    append(sb, "}");
                                    statements.pop();

                                    // Check for else branch
                                    final String prevLine = getLine(-1);
                                    if (prevLine.contains("jump")) {
                                        append(sb, "else");
                                        append(sb, "{");

                                        statements.push(StatementType.ELSE);
                                        jumps.add(prevLine.split("\\s")[1]);
                                    }
                                    break;
                                }
                                case ELSE: // If block is else
                                {
                                    statements.pop();

                                    // If else block is empty then remove it, otherwise just append end bracket
                                    final int elsePos = sb.lastIndexOf("else");
                                    if ((elsePos >= 0) && sb.substring(elsePos).replaceAll("\\s", "").equals("else{")) {
                                        sb.delete(elsePos, sb.length());
                                        sb.replace(sb.lastIndexOf("}") + 1, sb.length(), EOL);
                                        _indent--;
                                    } else {
                                        append(sb, "}");
                                    }

                                    // Check if current jump is also associated with "case"
                                    if (getLine(-2).contains("jump") && getLine(1).contains("push_reg_sp")) {
                                        append(sb, "break;");
                                        append(sb, "}");
                                        statements.push(StatementType.CASE);
                                    }
                                    break;
                                }
                                case FOR: // If block is "for"
                                {
                                    append(sb, "}");
                                    statements.pop();
                                    break;
                                }
                                case SWITCH: // If block is switch
                                {
                                    // Check last case should have "break" statement
                                    if (getLine(-2).contains("jump")) {
                                        append(sb, "break;");
                                    }

                                    // If "case" is not empty add end bracket, otherwise remove "case" start bracket
                                    final int pos = sb.lastIndexOf(":");
                                    if (sb.substring(pos).replaceAll("\\s", "").length() != 2) {
                                        append(sb, "}");
                                    } else {
                                        sb.delete(pos + 1, sb.length());
                                        sb.append(EOL);
                                        _indent--;
                                    }

                                    // Check if switch should contain another "case" statement, otherwise append "switch" end bracket
                                    if (getLine(1).contains("push_reg_sp")) {
                                        statements.push(StatementType.CASE);
                                    } else {
                                        switchData.pop();
                                        statements.pop();
                                        append(sb, "}");
                                    }
                                    break;
                                }
                                default:
                                    break;
                            }
                        } else {
                            // Set last unregistered jump (used for "while" statement)
                            lastJump = split[0];
                        }
                    }
                }
            }
        }
    }

    /**
     * Append {@code text} to buffer with indentation.
     *
     * @param sb   buffer where text should be appended
     * @param text text which should be appended
     */
    private void append(StringBuilder sb, String text) {
        append(sb, text, true);
    }

    /**
     * Append {@code text} to buffer with indentation.
     *
     * @param sb   buffer where text should be appended
     * @param text text which should be appended
     * @param eol  {@code true} append end of line, otherwise append it without EOL
     */
    private void append(StringBuilder sb, String text, boolean eol) {
        // Lower indentation
        if (text.startsWith("}")) {
            _indent--;
        }
        // Append text
        for (int i = 0; i < _indent; i++) {
            sb.append("\t");
        }
        sb.append(text);
        // Append end of line
        if (eol) {
            sb.append(EOL);
        }
        // Increment indentation
        if (text.equals("{")) {
            _indent++;
        }
    }

    /**
     * Get current line.
     *
     * @return line if pointer is not out of bounds, otherwise {@code null}
     */
    private String getLine() {
        return _lines.get(_currLine);
    }

    /**
     * Get line at {@code offset} distance from current line
     *
     * @param offset line offset
     * @return line if pointer is not out of bounds, otherwise {@code null}
     */
    private String getLine(int offset) {
        return _lines.get(_currLine + offset);
    }

    /**
     * Increment line pointer and return current line.
     *
     * @return line if pointer is not out of bounds, otherwise {@code null}
     */
    private String readLine() {
        return _lines.get(++_currLine);
    }

    private void pushBComparison(Stack<String> stack, boolean inverse) {
        final String val1 = stack.pop();
        final String val2 = stack.pop();
        switch (val1) {
            case "1":
                stack.push(inverse ? "!" + val2 : val2);
                break;
            case "0":
                stack.push(inverse ? val2 : "!" + val2);
                break;
            default:
                stack.push(ComparisonMapper.getInstance().get(val2, val1, inverse ? "!=" : "==", _importLibs));
                break;
        }
    }

    private void pushComparison(Stack<String> stack, String sign) {
        String val1 = stack.pop();
        String val2 = stack.pop();
        boolean oversign = (sign.equals("==") || sign.equals("!=")) && (val2.contains("-") || val2.contains("+"));
        if ((val2.charAt(0) == '(') && (val2.charAt(val2.length() - 1) == ')') && !oversign) {
            val2 = val2.substring(1, val2.length() - 1);
        }
        oversign = (sign.equals("==") || sign.equals("!=")) && (val1.contains("-") || val1.contains("+"));
        if ((val1.charAt(0) == '(') && (val1.charAt(val1.length() - 1) == ')') && !oversign) {
            val1 = val1.substring(1, val1.length() - 1);
        }
        if (_name.equals("ai_agit02_doom_archer_agit") && val2.equals("p_state != 3 & i_ai1"))
            val2 = "p_state != State.ATTACK & i_ai1";
        else if (_name.equals("warrior_corpse_vampire") && val2.equals("i_ai0") && val1.equals("1")) {
            stack.push("i_ai0 = 1;");
            return;
        }
        stack.push(ComparisonMapper.getInstance().get(val2, val1, sign, _importLibs));
    }

    /**
     * Get two values from stack concatenate them using {@code sign} character and put new value back.
     *
     * @param stack    stack where values are stored
     * @param sign     character used for concat
     * @param brackets put brackets around newly created string
     */
    private void pushArithmetic(Stack<String> stack, String sign, boolean brackets) {
        final String val1 = stack.pop();
        String val2 = stack.pop();

        final boolean oversign = (sign.equals("*") || sign.equals("/")) && (val2.contains("+") || val2.contains("-"));
        if ((val2.charAt(0) == '(') && (val2.charAt(val2.length() - 1) == ')') && !oversign) {
            val2 = val2.substring(1, val2.length() - 1);
        }
        String newVal = val2 + " " + sign + " " + val1;
        if (brackets) {
            newVal = "(" + newVal + ")";
        }
        stack.push(newVal);
    }

    /**
     * Get two values from stack concatenate them using logical {@code sign} character and put new value back to stack.
     *
     * @param stack stack where values are stored
     * @param sign  character used for concat
     */
    private void pushLogic(Stack<String> stack, String sign) {
        final String inverse = (sign.equals("&&")) ? "||" : "&&";
        final String val1 = stack.pop();
        String val2 = stack.pop();

        if ((val2.charAt(0) == '(') && (val2.charAt(val2.length() - 1) == ')') && !val2.contains(inverse)) {
            val2 = val2.substring(1, val2.length() - 1);
        }
        stack.push("(" + val2 + " " + sign + " " + val1 + ")");
    }

    /**
     * Check if last appended "if" statement should be interpreted as "else if".
     *
     * @param sb buffer that contains decompiled lines
     * @return {@code true} when last "if" should be "else if", otherwise {@code false}
     */
    private boolean isElseIf(StringBuilder sb) {
        // Get last "else" statement in buffer
        final int elsePos = sb.lastIndexOf("else");
        if (elsePos >= 0) // If any found then check how far is it from last "if"
        {
            final int ifPos = sb.lastIndexOf("if");
            final String substr = sb.substring(elsePos, ifPos).replaceAll("\\s", "");
            if (substr.equals("else{")) // When before last "if" statement is only "else{" (without whitespace characters) then replace it with "else if"
            {
                sb.replace(elsePos, ifPos, "else ");
                _indent--;
                append(sb, "{");
                return true;
            }
        }
        return false;
    }

    /**
     * Check if {@code jmpName} is start of "switch" statement.
     *
     * @param jmpName name of current jump
     * @return {@code true} when "switch" statement should be appended, otherwise {@code false}
     */
    private boolean isSwitch(String jmpName) {
        // Find line starting with jump name
        int offset = 1;
        while (!getLine(offset).trim().equals(jmpName)) {
            offset++;
        }

        // Check if jump is switch or not
        return getLine(offset - 1).contains("jump") && getLine(offset + 1).contains("push_reg_sp");
    }

    /**
     * Check if {@code jmpName} is start of "while" statement.
     *
     * @param jmpName name of current jump
     * @param lastJmp last unregistered jump name
     * @return {@code true} when jump should be interpreted as "while", otherwise {@code false}
     */
    private boolean isWhile(String jmpName, String lastJmp) {
        // Find line starting with jump name
        int offset = 0;
        while (!getLine(offset).trim().equals(jmpName)) {
            offset++;
        }

        // Check if jump is while or not
        final String[] prevLine = getLine(offset - 1).split("\\s");
        return prevLine[0].equals("jump") && prevLine[1].equals(lastJmp);
    }

    /**
     * Remove brackets from beginning and end of string.
     *
     * @param value string value
     * @return striped value
     */
    private String stripBrackets(String value) {
        final int length = value.length() - 1;
        if (value.charAt(0) == '(' && value.charAt(length) == ')') {
            value = value.substring(1, length);
        }
        return value;
    }

    public Set<String> getParams() {
        return _params;
    }


    private String getVarWithCaller(String caller, String key) {
        if (key.equals("id") || key.equals("dbid"))
            return "getObjectId()";
        else if (caller.equals("npc0") && key.equals("sm"))
            return null;
        else if (caller.equals("h0") && key.equals("creature"))
            return "attacker";
		else if (caller.equals("actor") && key.equals("master"))
			return "getLeader()";
        return getVar(key);
    }

    private String getParam(String key) {
        switch (key) {
            case "myself":
            case "x":
            case "y":
            case "z":
            case "level":
                return key;
            default:
                return getVar(key);
        }
    }

    private String getVar(String key) {
        String result = key;
        switch (key) {
            case "gg": {
                result = "AiUtils";
                _importLibs.add("AiUtils");
                break;
            }
            case "myself": {
                result = null;
                break;
            }
            case "sm": {
                result = "actor";
                _handlerVars.add("actor");
                break;
            }
            case "hp": {
                result = "currentHp";
                break;
            }
            case "mp": {
                result = "_currentMp";
                break;
            }
            case "is_pc": {
                result = "isPlayer()";
                break;
            }
            case "master": {
                result = "getPlayer()";
                break;
            }
            case "member_count": {
                result = "getMemberCount()";
                break;
            }
            case "max_hp": {
                result = "getMaxHp()";
                break;
            }
            case "alive": {
                result = "isAlive()";
                break;
            }
            case "top_desire_target": {
                _handlerVars.add("top_desire_target");
                _importLibs.add("Creature");
                break;
            }
            case "h0": {
                break;
            }
            case "summon_type": {
                result = "getSummonType()";
                break;
            }
            case "ai": {
                result = "getAI().getClass().getName()";
                break;
            }
            case "respawn_time": {
                result = "_respawn_minion";
                break;
            }
            case "in_peacezone": {
                result = "isInZonePeace()";
                break;
            }
            case "weight_point": {
                result = "_desirePoint";
                break;
            }
            case "npc_class_id": {
                result = "getClassId()";
                break;
            }
            case "occupation": {
                result = "getClassId()";
                break;
            }
            case "class_id": {
                result = "getClassId()";
                break;
            }
            case "race": {
                result = "getRaceId()";
                break;
            }
            case "level": {
                result = "getLevel()";
                break;
            }
            case "subjob_id": {
                result = "isSubClassActive()";
                break;
            }
            case "x": {
                result = "getX()";
                break;
            }
            case "y": {
                result = "getY()";
                break;
            }
            case "z": {
                result = "getZ()";
                break;
            }
            case "private": {
                result = "privat";
                break;
            }
            case "boss":
                _handlerVars.add("boss");
                break;
            case "boss_id":
                result = "getLeader()";
                break;
            case "name":
                result = "getName()";
                break;
            case "code":
                result = "getCode()";
                break;
            case "builder_level": {
                result = "getAccessLevel()";
                break;
            }
            case "equiped_weapon_class_id": {
                result = "getWeaponClass()";
                break;
            }
            case "attack_type": {
                result = "getWeaponType()";
                break;
            }
            default: {
                return result;
            }
        }
        return result;
    }

    private boolean paramExists(String param) {
        String extend = _name;
        while (true) {
            extend = _reader._classTree.get(extend);
            if (extend == null)
                break;
            if (_reader._paramTree.get(extend).contains(param))
                return true;
        }
        return false;
    }
}