package me.moruto;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;

public class GUI extends Application {
    private File inputFile;
    private File outputFile;
    private File fileToInject;
    private TextField mainClassInput;
    private TextField mainMethodInput;
    private static TextArea consoleOutput;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Jar Injector");

        Image icon = new Image(getClass().getResource("/icon.png").toString());
        primaryStage.getIcons().add(icon);

        BorderPane borderPane = new BorderPane();

        HBox topButtons = new HBox(10);
        topButtons.setPadding(new Insets(10, 10, 10, 10));
        topButtons.setAlignment(Pos.CENTER_LEFT);

        Button settingsButton = new Button("Settings");
        Button injectButton = new Button("Inject");

        topButtons.getChildren().addAll(settingsButton, injectButton);
        borderPane.setTop(topButtons);

        GridPane settingsGrid = new GridPane();
        settingsGrid.setPadding(new Insets(10, 10, 10, 10));
        settingsGrid.setVgap(10);
        settingsGrid.setHgap(10);

        Label inputLabel = new Label("Input Path:");
        GridPane.setConstraints(inputLabel, 0, 0);

        TextField inputPathField = new TextField();
        inputPathField.setPromptText("Enter input path");
        GridPane.setConstraints(inputPathField, 1, 0);

        Button inputPathButton = new Button("Select File");
        GridPane.setConstraints(inputPathButton, 2, 0);

        Label outputLabel = new Label("Output Path:");
        GridPane.setConstraints(outputLabel, 0, 1);

        TextField outputPathField = new TextField();
        outputPathField.setPromptText("Enter output path");
        GridPane.setConstraints(outputPathField, 1, 1);

        Button outputPathButton = new Button("Select File");
        GridPane.setConstraints(outputPathButton, 2, 1);

        Label fileToInjectLabel = new Label("File to Inject:");
        GridPane.setConstraints(fileToInjectLabel, 0, 2);

        TextField fileToInjectField = new TextField();
        fileToInjectField.setPromptText("Enter file to inject");
        GridPane.setConstraints(fileToInjectField, 1, 2);

        Button fileToInjectButton = new Button("Select File");
        GridPane.setConstraints(fileToInjectButton, 2, 2);

        Label injectionTypeLabel = new Label("Injection Type:");
        GridPane.setConstraints(injectionTypeLabel, 0, 3);

        Label mainClassLabel = new Label("Main Class:");
        GridPane.setConstraints(mainClassLabel, 0, 4);

        mainClassInput = new TextField();
        mainClassInput.setPromptText("Enter Main Class");
        GridPane.setConstraints(mainClassInput, 1, 4);

        Label mainMethodLabel = new Label("Main Method:");
        GridPane.setConstraints(mainMethodLabel, 0, 5);

        mainMethodInput = new TextField();
        mainMethodInput.setPromptText("Enter Main Method");
        GridPane.setConstraints(mainMethodInput, 1, 5);

        settingsGrid.getChildren().addAll(
                inputLabel, inputPathField, inputPathButton,
                outputLabel, outputPathField, outputPathButton,
                fileToInjectLabel, fileToInjectField, fileToInjectButton,
                mainClassLabel, mainClassInput,
                mainMethodLabel, mainMethodInput
        );

        VBox injectBox = new VBox(10);
        injectBox.setAlignment(Pos.CENTER);
        injectBox.setPadding(new Insets(20, 20, 20, 20));

        consoleOutput = new TextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setWrapText(true);
        consoleOutput.setPrefHeight(150);
        consoleOutput.setPrefWidth(350);

        Button injectAction = new Button("Inject");
        injectAction.setOnAction(e -> inject());

        injectBox.getChildren().addAll(consoleOutput, injectAction);

        borderPane.setCenter(settingsGrid);

        settingsButton.setOnAction(e -> borderPane.setCenter(settingsGrid));
        injectButton.setOnAction(e -> borderPane.setCenter(injectBox));

        inputPathButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Jar Files", "*.jar"));
            inputFile = fileChooser.showOpenDialog(primaryStage);
            if (inputFile != null) {
                inputPathField.setText(inputFile.getAbsolutePath());
            }
        });

        outputPathButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Jar Files", "*.jar"));
            outputFile = fileChooser.showSaveDialog(primaryStage);
            if (outputFile != null) {
                outputPathField.setText(outputFile.getAbsolutePath());
            }
        });

        fileToInjectButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Jar Files", "*.jar"));
            fileToInject = fileChooser.showOpenDialog(primaryStage);
            if (fileToInject != null) {
                fileToInjectField.setText(fileToInject.getAbsolutePath());
            }
        });

        Scene scene = new Scene(borderPane, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void inject() {
        if (inputFile == null) {
            log("Please select the input file.");
            return;
        }

        if (outputFile == null) {
            log("Output file is invalid, defaulting to \"input path + \"-injected\"\"");
            outputFile = new File(inputFile.getAbsolutePath().replace(".jar", "-injected.jar"));
        }

        if (fileToInject == null) {
            log("Please select the file to inject.");
            return;
        }

        String output = outputFile.getAbsolutePath();
        String mainClass = mainClassInput.getText();
        String mainMethod = mainMethodInput.getText();
        consoleOutput.clear();

        if (!inputFile.exists()) {
            log("Input file is missing!");
            return;
        }

        JarLoader inputJarLoader = new JarLoader();
        boolean mainLoaded = inputJarLoader.loadJar(inputFile);
        if (!mainLoaded) {
            log("Error loading the main jar. Please try again!");
            return;
        } else log("Main Jar successfully loaded!");

        JarLoader injectionJarLoader = new JarLoader();
        boolean loaded = injectionJarLoader.loadJar(fileToInject);
        if (!loaded) {
            log("Error loading the Injection jar. Please try again!");
            return;
        } else log("Injection Jar successfully loaded!");

        for (ClassNode classNode : injectionJarLoader.classes) {
            System.out.println(classNode.name);
        }

        System.out.println(injectionJarLoader.classes.size());

        JarInjector.inject(mainClass.replace(".", "/"), mainMethod, inputJarLoader.classes);
        inputJarLoader.classes.addAll(injectionJarLoader.classes);
        inputJarLoader.resources.addAll(injectionJarLoader.resources);
        
        inputJarLoader.saveJar(output);
        GUI.log("Successfully saved the jar!");
    }

    public static void log(String message) {
        Platform.runLater(() -> consoleOutput.appendText(message + "\n"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
