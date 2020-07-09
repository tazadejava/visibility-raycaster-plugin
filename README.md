# visibility-raytracing-plugin
Spigot plugin developed for Genesis Recommendation System UROP, Summer of 2020. The key raytracing algorithm components were extracted from the main project and ported into this plugin for ease of use and transferability. 

# Quick Start: how to test the raycasting visibility algorithm

**Note that you do not need to clone this repo to test the raycasting algorithm. Simply download the server file to test the algorithm, and you can use the repo to view the source and see how the algorithm is implemented.**

Server file link: https://drive.google.com/file/d/1TPjnvjTQzYshLtgaBUA0BFFYqv1PBt5C/view?usp=sharing 

1) UNZIP the VisibilityRaytracingServer.zip file. This will hold the Minecraft server that you will need to run and join.

2) Within the VisibilityRaytracingServer folder, there should be a start.sh file. Execute this file and the server should start up (this works for Mac and Linux. For windows computers, use the RUN.bat file instead). The server is ready when it states "Done (XXXs)". The server is on 1.16.1.

3) Open up Minecraft and if you are on the same computer as the server, join via the address: localhost

4) If you are not in creative mode or want to change your gamemode to fly around the map, first in the terminal type "op NAME", where NAME is your username, then you can type in Minecraft "gamemode creative/survival" to change your gamemode. **You have to op yourself in order to use the raycasting algorithm.**

5) To start raycasting, you can type any of the following commands in Minecraft:

- "/raycast continuous"
    - This will start a continuous raycast that will update every 4 ticks (there are 20 ticks in a second)
    - This will "paint" glass onto all the blocks that you have seen since the command started
        - Simply walk around to see which blocks are being viewed
    - When you want to stop, SNEAK (hold shift) for about 2 seconds until it says "Abort raycaster"
    - When you want to reset the blocks to normal, type "/raycast reset"
- "/raycast discrete"
    - This is like the first one, but it will only raycast once before resetting the blocks to normal, then it raycasts again.
    - WARNING: due to the way Minecraft renders client-side blocks, this flickers extremely quickly. I would not recommend using this command if this would bother you, as it can be visually extremely difficult. Use either the raycast command above or see the "raycast once" command below for alternatives.
- "/raycast once"
    - This will only run the algorithm once, so you can see what is currently visible at your current view.
    - Again, type "/raycast reset" to reset the blocks to normal.
- "/raycast save"
    - This will save the blocks that are currently being viewed as glass to a JSON file.
    - You can find it within the plugins/VisibilityRaytracerPlugin/raycasts folder, where the file is timestamped.
- "/raycast reset":
    - Resets the blocks currently being rendered as glass to their normal state.
    - Note that signs may appear to lose their text, but this is only a client-side issue, and is restored if the player logs out and in again.
    
# Important classes that implement this algorithm

## CommandHandler

This class is in charge of handling all commands that start with the word "raycast". You can see that it calls a switch method to determine which command was used, then it runs the algorithm depending on which raycasting algorithm was used (see startRaycast method and restoreBlocks method to see how we can interact with the results of the algorithm) 

## PreciseVisibleBlocksRaycaster

This class implements the raycasting algorithm. Simply run getVisibleBlocks method and you will get a list of blocks that are visible to the player. There are a few helper methods that help determine the raycasting algorithm, but essentially it will run like how I described during the meeting:

1) See what block the player is currently looking at. There is a max distance constant to save performance and a timeout function to also save performance

2) Look at the air block that is in front of this block the player is looking at

3) Now, expand out in all 6 directions (north west up down east south) and look for air blocks that have a solid block (anything but air) next to it. Add to an open list, then continue to spread out (this algorithm is similar to A* algorithm, implemented in a 3D environment)

4) If air has no solid block next to it, then we forbid it from expanding out to another air block that has no solid block next to it. This makes the algorithm prefer air blocks that are next to solid blocks, so we can ensure best performance and only blocks that are visible to the player.

5) The FOVBounds class will calculate vectors to determine if a block is in the FOV of a player. It is a mostly accurate estimate.

6) Then, we will raycast from the player eye location to the block itself, based on which direction we see it in (it will raycast to the face of the block). If we hit it, it is visible. Otherwise, we check the hit block for blocks around it "fuzzy detection" to see if we missed it slightly (loop the blocks around the hit block), then add anyways.

7) The hyperPrecision mode will basically shoot out up to 5 rays from the player eye location to different parts of the block to accurately check if we can see the block or not. It is also slightly more computationally intense, but it leads to extremely accurate results.

## RayTracerPlugin

This class simply registers the command to the server when the plugin is loaded.

# Remarks

Feel free to contact me if anything is confusing or if there are any questions about the plugin (or plugins in general).
