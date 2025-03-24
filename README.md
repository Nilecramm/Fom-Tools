<img width="1469" alt="Capture d’écran 2025-03-24 à 17 11 37" src="https://github.com/user-attachments/assets/aa7ae4ae-29fa-4fba-a8dd-9551bb5f0180" />

# Disclaimer  
This app was developed for fun. It probably contains a lot of bugs, and I do not wish to fix them. You have the source code here—if you want, you can do stuff with it.  

I tested it on macOS (ARM) and Windows, and it worked fine.  

Some animations just crash.  

There are some visual artifacts sometimes—try pressing pause/remain or restart the app itself.  

## Features  
You can:  
- Select a folder containing your character's sprites.  
- Select your own editing tool (the `.exe` or `.app` of it).  
- Select any animation/view angle of the character.  
- See individual sprites rendered live and edit them (I recommend pausing to do so).  
- Slow down/accelerate the animation speed.  
- Play with the scale of the animation.  
- Edit the sprites live (just press the "refresh" button for them to update).  

## Requirements  

### 1) JAVA FX  
You need to download the JavaFX SDK 21 for your computer.  
Go to this link: [https://gluonhq.com/products/javafx/](https://gluonhq.com/products/javafx/)  
Select version 21 (currently, 21.0.6), and install the **SDK type** (not the jmod one).  
You can then extract it and drag it into the root folder of the app, at the same level as the `.jar` and the launch executables.  

### 2) JAVA (JDK 21)  
If you have Java, make sure you have the JDK 21 version. If not:  
Go to this link: [https://www.oracle.com/java/technologies/downloads/](https://www.oracle.com/java/technologies/downloads/)  

### 3) `par_output.json`  
Extract this file from your game. I do not wish to share it since it is probably copyrighted.  
You can find it here:  
```
Steam\steamapps\common\Fields of Mistria\animation\generated\par_output.json
```  
Add it to the root folder.  

## Launching the app  
You will need to use the command line. If you are on Windows, you can try the `launch.bat` file, and if it works, you're good to go.  
On macOS, you have the similar `launch.sh`—use a terminal and open it with the command:  
```bash
./launch.sh
```  

Sometimes, it doesn't work.  
Add this option in the command line and try again (edit the executable link directly so you won’t need to write it again):  
`-Dprism.order=sw`. Like this:  
```bash
java -Dprism.order=sw --module-path javafx-sdk-21.0.6/lib --add-modules javafx.controls,javafx.fxml --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED -jar FomTools-1.0-SNAPSHOT.jar
```  

### Using the app  
For your character sprites, you can organize them however you want. The only requirement is:  
- A folder containing all of them.  

The program will find the sprites itself, even if you organize them in subfolders.  
