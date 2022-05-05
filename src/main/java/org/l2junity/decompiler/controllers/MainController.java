package org.l2junity.decompiler.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.l2junity.decompiler.ThreadPoolManager;
import org.l2junity.decompiler.reader.AiReader;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * Controller for Main window
 * @author malyelfik
 */
public final class MainController
{
	@FXML
	private TextArea logArea;
	@FXML
	private Button decompileOne;
	@FXML
	private Button decompileClass;
	
	@FXML
	protected void initialize()
	{
		if (!Files.exists(Paths.get(getAIPath())))
		{
			appendToLog("Cannot find ai.obj file.");
			disableButtons(true);
		}
	}
	
	@FXML
	public void handleDecompileOneClick(ActionEvent event)
	{
		ThreadPoolManager.getInstance().execute(new AiReader(this, true));
	}
	
	@FXML
	public void handleDecompileClassClick(ActionEvent event)
	{
		ThreadPoolManager.getInstance().execute(new AiReader(this, false));
	}
	
	@FXML
	public void translateSkillDialog(ActionEvent event)
	{
		try
		{
			final AnchorPane pane = FXMLLoader.load(getClass().getResource("/Skill.fxml"));
			final Stage stage = new Stage();
			stage.initOwner(logArea.getScene().getWindow());
			stage.setTitle("Skill ID convertor");
			stage.setResizable(false);
			stage.setScene(new Scene(pane));
			stage.show();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void disableButtons(boolean val)
	{
		decompileOne.setDisable(val);
		decompileClass.setDisable(val);
	}
	
	public void appendToLog(String msg)
	{
		appendToLog(msg, true);
	}
	
	public void appendToLog(String msg, boolean newLine)
	{
		if (newLine)
		{
			msg += "\n";
		}
		logArea.appendText(msg);
	}
	
	public String getAIPath()
	{
		return "./ai_hf.obj";
	}
}