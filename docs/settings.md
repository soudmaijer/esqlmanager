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

Settings > JDBC driver settings shows per server type where its driver comes from, the driver class, the URL and the quote characters.

The PostgreSQL (BSD-2-Clause) and SQL Server (MIT) drivers are included. The MySQL and Oracle drivers are not, because of their licences: MySQL Connector/J is GPLv2 with the Universal FOSS Exception, the Oracle driver (ojdbc11) is under the Oracle Free Use Terms and Conditions (FUTC). They are downloaded from Maven Central when they are first needed:

* Connecting to, or testing a connection with, a MySQL or Oracle server whose driver is missing asks "Download MySQL driver (2.6 MB, licence GPLv2 with the Universal FOSS Exception)?". Download fetches it with a progress window and then goes on with the connection; Cancel stops.
* The Driver group of JDBC driver settings shows the status (Bundled, Downloaded, Own jar or Not installed) and has a Download button.

Each driver is downloaded at a fixed version (MySQL Connector/J 26.7.0, ojdbc11 23.26.3.0.0) into `~/.esqlmanager/drivers` and checked against the SHA-256 checksum that is built into eSQLManager, when it is downloaded and every time it is loaded. A file that does not match is deleted. Behind a proxy or without internet, place the jar in that folder yourself, or choose any driver jar under **Own jar**: that jar is then used instead (it is stored in `driver.xml` as `<driverJar>`).

## Files

The application reads and writes its configuration in `conf/`:

| File | Contents |
|---|---|
| `profiles.xml` | Connection profiles |
| `settings.xml` | Preferences: appearance, editor font size, default folder and default file encoding |
| `driver.xml` | JDBC driver class, URL and own driver jar per server type |
| `datatypes.xml` | Column types per server type |

## Log file

Everything the application logs, with the full details of every error, is written to `esqlmanager.log` in `~/.esqlmanager/logs` (on Windows `C:\Users\<name>\.esqlmanager\logs`). The output panel shows an unexpected error as one line ending in "(details in esqlmanager.log)". **Help > Open log folder** opens the folder. A new file is started each day or at 10 MB, and the last five old files are kept.
