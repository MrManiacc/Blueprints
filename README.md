# *\*blueprints is currently undergoing a full re-write. stay tuned.**

# What is bpm?

Blueprints is an all in one automation solution for Minecraft. It supports GUI based node programming so anyone can
create powerful automated tasks without any prior programming knowledge. In a matter of seconds automate transfers
between containers independent of each other, create mob grinder, and so much more! This mod is in a very early stage
and will have many more bug fixes and features to come!

## What can it do?

Multi-function support (Create multiple functions in a single block that run independently of each other!) All in one
automation system (From containers, to mobs, and beyond this is the ultimate tool for any task automation in game) GUI
based programming! (Create automated functions through GUI based node programming.) How's it work: Craft a 'singularity'
block and jump right into developing your automates systems by opening it up and selecting all the options that suit
your needs!

## How does it work?

Blueprints uses a node based system. You can use link nodes to specify a real in world link to either an inventory of
items, fluids, or energy (more to come maybe). You can extract items at a specified rate using a ticking node, and you
can filter the items extracted. There's a buffer node for storing near infinite items, it displays it's current
inventory in the node gui, and can have it's buffer inserted into/extracted from (also can be filtered). There's a node
called the "hopper node" which as it's name suggests, allows you to select a location in world and pick up the items (
can be filtered). This can be inserted into a link node (real world inventory), stored in a buffer, or kept inside the
internal hopper buffer. There's also a player breaker node that also you to set the item in the fake player's hand and
break a block at the selected location, this is useful for things like ores that require a pickaxe of a certain tier.
Finally (for now) there's a user node, which as it's name suggests allows you to right click. The user node also has an
internal buffer allowing you to push items into it, meaning you could say automate a wheat farm by inserting wheat into
one user node, bone meal into a another then just setting the tick speed.

## Images

![gui image](https://i.imgur.com/IneSPmL.png)

## Notice

This mod is still in an alpha stage. While the core features are present, there is still a lot of work that must be
done. That this mod allows for unfathomable amounts of customization and as such in the future is going to need some
balancing. In it's current state most would consider this extremely "op". I'm also currently looking for people to help
with the project in either programming and or modeling/textures. This mod **
requires** [KotlinForForge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge) 
