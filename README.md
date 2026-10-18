# Ground Loot Menu

Shows nearby ground items in a movable panel and highlights the pile you select.

Items within 15 tiles are listed in a 4 by 4 grid. The panel shows each stack’s Grand Exchange value, plus the Grand Exchange and high-alch totals for everything that passes the filters. Clicking a slot marks that item. The marked tile is highlighted, and that item becomes the left-click on its pile. Click the same slot again to unmark it, or set a timer that clears the mark on its own. You still pick the item up by clicking it in the game.

## Features

- Lists takeable ground items within 15 tiles.
- Shows up to 16 stacks, with the full match count and value totals.
- Sorts by Grand Exchange value, quantity, or name.
- Hides ignored, unpriced, or low-value stacks. An always-show list bypasses those filters.
- Left-click a slot to select it. The tile is marked, and other items on that pile are deprioritized so the selection is the left-click. Click that slot again to unmark it.
- Right-click a slot to add or remove that item from the ignore list or the always-show list.
- Hold Alt and drag the panel to move it.
- Collapses to a satchel button, and can collapse automatically when nothing matches.

## Configuration

| Setting | Description | Default |
| --- | --- | --- |
| Hide low-value items | Hide stacks below the minimum total Grand Exchange value. | On |
| Minimum total GE value | Minimum Grand Exchange value of the whole stack, in gp. | 100 |
| Hide unpriced items | Hide items with no positive Grand Exchange price. | Off |
| Ignore list | Comma-separated item names to hide, unless they are on the always-show list. | Empty |
| Always show items | Comma-separated item names that bypass the ignore, minimum-value, and unpriced filters. | Empty |
| Sort order | Value, quantity, or name. | Value |
| Show quantity | Draw the stack size on each icon. | On |
| Auto-collapse when empty | Collapse the panel when no items match, and expand it when some do. | On |
| Show GE total | Show the total Grand Exchange value of matching items. | On |
| Show high-alch total | Show the total high-alchemy value of matching items. | On |
| Clear highlight after | Seconds before a selected tile is unmarked. 0 keeps the mark until you click that item again. | 0 |

Names in both lists are matched without regard to case. Separate names with commas or new lines.
