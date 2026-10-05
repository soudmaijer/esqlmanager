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

## Editor, folder and encoding

Settings > Preferences also holds:

| Setting | Effect |
|---|---|
| Editor font size | The font size (8 to 32) of the query editor, the output panel and the other SQL editors. It applies at once. |
| Default folder | The folder the export, import, upload and download file windows start in. Empty uses the folder the system chooses. |
| Default file encoding | The encoding the export and import windows start with (UTF-8 unless changed). |

## JDBC drivers

Settings > JDBC Driver settings shows the driver class and URL per server type. The drivers for MySQL, PostgreSQL, SQL Server and Oracle are included.

## Files

The application reads and writes its configuration in `conf/`:

| File | Contents |
|---|---|
| `profiles.xml` | Connection profiles |
| `settings.xml` | Preferences: appearance, editor font size, default folder and default file encoding |
| `driver.xml` | JDBC driver class and URL per server type |
| `datatypes.xml` | Column types per server type |
