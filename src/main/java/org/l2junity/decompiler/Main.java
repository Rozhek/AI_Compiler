package org.l2junity.decompiler;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application
{
	@Override
	public void start(Stage primaryStage)
	{
		try
		{
			Pane root = (Pane) FXMLLoader.load(getClass().getResource("/Main.fxml"));
			primaryStage.setTitle("AI decompiler");
			primaryStage.setResizable(false);
			primaryStage.setScene(new Scene(root, 600, 400));
			primaryStage.show();
			
			ThreadPoolManager.getInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void stop()
	{
		ThreadPoolManager.getInstance().shutdown();
	}
	
	public static void main(String[] args)
	{
		launch(args);
	}
}
