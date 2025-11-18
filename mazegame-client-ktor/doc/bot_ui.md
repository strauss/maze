# Enrich your bot with UI features

It is possible to define some UI features for your bot.
You can add a small control panel to the overall UI.
You can also create an in-place visualization for your bot.

## Control panel

In order to define a control panel, your strategy object requires to override the function `getControlPanel`.
This can be any kind of `JPanel`.
If you provide one, the UI adds a small button with a helm symbol (⎈) in the bottom right of the status bar
(this button is also visible, if you enable the server controls).

When you click on that button, the control panel is placed at the bottom of the right area that will appear.
Remember, you have to share that space with the server controls if they are enabled.

All Swing rules apply.
So do not block the EDT or else your maze will freeze.

## Visualization

The more interesting part is the possibility to implement your own visualization.
In order to do so, you need to override the function `getVisualizationComponent`.

In this case, the corresponding Swing component has to be a specialization of
[VisualizationComponent](../src/main/kotlin/de/dreamcube/mazegame/client/maze/strategy/VisualizationComponent.kt).
This class is automatically registered as event listener.
Be careful when initializing it, because several objects won't be available right away.

The visualization is drawn right on top of the visible maze.
If you start a dummy bot, you can actually see an example and the available space.

### Information provided by the UI

Those attributes have setters, but they are intended to be called by the UI only.
So just use the getters, or else your visualization might break or glitch.

- `visualizationEnabled`: can be used to decide on expensive operations. If the visualization is disabled, you don't
  need to perform them.
- `colorDistributionMap`: maps the player ids to their colors. It's not required to use it directly. Just use the
  function `getPlayerColor`. It gives you "black" as fallback, if you give an unknown player id.
- `selectedPlayerId`: if set, it contains the id of the selected player. The selected player is also the "marked"
  player. They are also highlighted
- `zoom`: the current zoom factor. Required for calculating the size of elements you want to draw.
- `offset`: the current offset. It is the coordinate of the top left point of the visible maze. It is mandatory for
  drawing objects relative to the maze. You cannot go without, if you want to mark stuff inside the maze. The value is
  updated using the function `updateOffset`, which is also intended to be called by the UI.

### Draw the visualization

In order to actually draw your visualization, you have to override the function `paintComponent`.
The given `Graphics` reference can be cast to `Graphics2D` for more advanced drawing functions.
This function is always executed on the EDT.
So do not perform costly operations here.

A good way to utilize the visualization is providing it with "new information" from your strategy and call the function
`repaint`, whenever you want to update it.

### Examples

Things that are worth visualizing:

- Mark the selected target
- Cross out discarded target(s)
- The calculated path to the target
- Calculated values for each field (e.g., heat map)
    - This can be enriched with semi-transparent color
- Statistics

### Final notes

- It is also possible to use the control panel for controlling the visualization (e.g., dis- or enabling certain aspects
  of it)
- A visualization might sound "too complicated", but it can actually help with debugging. It is certainly better than
  using the console.
- Be creative, but don't overdo it :-)