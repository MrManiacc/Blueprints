## Top priority
* Add the block/face selection back, but make it look better this time maybe
* Add functionality to the extraction node/all the nodes
* Add an inventory/buffer node that is bound to a real container inventory that can be accessed from the imgui node
* Make a console dockable window with imgui for logging data to the user.
* evaluate the need for the properties window (do we want to keep properties soley on the node it's self or push certain
  things to the side pane)

## Medium priority
* Create an auto crafter node that uses jei to search items.
* Add a redstone node that acts an enable/disable variable input for certain nodes like the TickNode
* Makes nodes group-able
* Make nodes copy/paste-able
* Add a way to check if the given pin is even able to connect to another pin. (a tick node output pin shouldn't be the
  input of a link node's input pin for example)
* Add a filter node
* Add comment node

## Low priority

* Add a player interaction node for in world automation (see cylic's item user for info)
* Unify the node style - uniform colors, and icons that make sense.
* Add more icon types/icon renderers.
* Add some introspection like if the player wanted to only execute x if y is true. For example the player may only want
  to export bones from an inventory until there's say 20 left.
  

## Lowest priority

* Figure out how to make the mod work 100% on macosx and linux.
* Create unit tests perhaps?
* Optimize more?
