package org.l2junity.decompiler.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * @author malyelfik
 */
public class SkillController
{
	@FXML
	private TextField textOffId;
	
	@FXML
	private TextField textClientId;
	
	@FXML
	public void onCloseButton(ActionEvent event)
	{
		textClientId.getScene().getWindow().hide();
	}
	
	@FXML
	public void onTranslateButton(ActionEvent event)
	{
		try
		{
			final int offId = Integer.parseInt(textOffId.getText());
			final int skillId = offId >> 16;
			final int skillLevel = offId & 0xFFFF;
			if (skillId > 0 && skillLevel > 0 && skillLevel < 0x10000)
			{
				textClientId.setText(skillId + " " + skillLevel);
			}
			else
			{
				textClientId.setText("Undefined");
			}
		}
		catch (Exception e)
		{
			textClientId.setText("Undefined");
		}
	}
}