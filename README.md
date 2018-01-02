# AdminTools #

<p>AdminTools is a player management suite for BungeeCord that allows server administrators to easily deal with player punishments, infractions, etc.</p> 

## Features ##
The plugin has the following features:
 * Warning players
 * Muting players
 * Banning players
 * Issuing IP bans
 * Blacklisting players
    * Only available through BungeeCord console
    * Accomplished by prefixing `blacklist` to the ban reason
 * Viewing alternate accounts a player has logged in with
 
#### Commands ####
 * `/alts <player> [page]` - Views a list of accounts that have logged in with a matching IP to the user
 * `/ban <player> [duration] <reason>` - Bans a player for a specific reason. If a duration is not set the ban will be permanent
    * `duration` must be of format `<time><token>...` where token is one of the following: `d`, `h`, `m`, `s`. For example: `1d2h3m5s`
 * `/info <player> [page]` - Displays past infraction information of players
 * `/mute <player> [duration] <reason>` - Mutes a player for a specific reason
    * `duration` has the same specifier as the `ban` command
 * `/unban <player> [reason]` - Unbans a player
 * `/unblacklist <player>` - Removes the blacklist of a player. Only executable by console.
 * `/unmute <player> [reason]` - Unmutes a player
 * `/warn <player> <reason>` - Gives a player a warning
 
## License ##
This software is available under the following licenses:

* MIT