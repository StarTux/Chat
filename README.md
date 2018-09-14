# Chat
Cross-server chat with channels. This plugin organizes chat in channels which players may leave and join any time. Private messages are implemented as such a channel. The main class provides a handful of public API functions which are subject to change as this plugin is not publicly distributed at this time.

![Hello Chat](https://i.imgur.com/iJ9wa5V.jpg)

## Features
- Freely configurable channels via SQL database.
- Global, local, party, and private messages.
- Large player-side customization, starting from chat colors to selecting displayed tabs.
- Title support via the [Titles](https://github.com/StarTux/Title) plugin.
- Players may ignore each other if they wish
- Cross server chat via the [Connect](https://github.com/StarTux/Connect) plugin.
- Full chat log to database.

## Commands
There is a global chat command, `/ch`, as well as shortcuts for every channel. Each channel can be messaged with its command, or focused on by using it without a message. With the standard set of channels, this would look as follows.
- `/chat` *or* `/ch` - Main chat interface with documented, clickable subcommands.
- `/g` - Speak in, or focus on, the **global** channel, which is visible on all servers.
- `/l` - Speak in, or focus on, the **local** channel, which is only visible within a 500 block distance.
- `/tell` *or* `/msg` *or `/pm` - Send a private message to someone. You can also focus a private conversation.
- `/reply` *or* `/r` - Reply to a previous private message from someone.
- `/party` - Party chat options. Create a party, join an existing party, or leave.
- `/p` - Speak in the selected party channel, or focus party chat.

## Customization
Players can customize their chat experience with the command interface, which provides easily clickable buttons in chat. The command to get there is `/ch set`, but it's also accessible via the `/ch` main menu. Options include colors, displayed tags, bracket style, and notification sounds. You can have a dog bark or cat meow at you when your favorite channel contains a new message.

![Chat Settings](https://i.imgur.com/YfBKWfR.jpg)