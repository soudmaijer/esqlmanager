# Settings

[Back to the help index](index.md)

## Appearance

Settings > Preferences > Appearance chooses the look:

| Choice | Look |
|---|---|
| Follow the system | Light or dark, as the operating system is set |
| Light | Always light |
| Dark | Always dark |
| Native | The look of the operating system (slow when resizing on macOS) |

The choice applies at once, also to the query editor and the designer. On macOS the menu is in the screen menu bar. The icons are vector icons from [Lucide](https://lucide.dev) and follow the theme.

## JDBC drivers

Settings > JDBC Driver settings shows the driver class and URL per server type. The drivers for MySQL, PostgreSQL, SQL Server and Oracle are included.

## Files

The application reads and writes its configuration in `conf/`:

| File | Contents |
|---|---|
| `profiles.xml` | Connection profiles |
| `settings.xml` | Preferences, such as the appearance |
| `driver.xml` | JDBC driver class and URL per server type |
| `datatypes.xml` | Column types per server type |
