package nl.errorsoft.esql.connection;

/**
 * A connection that is active on the server, as shown in the process list.
 *
 * @param time seconds the process has been in its current state
 */
public record ServerProcess(String id, String user, String host, String database, String command, String time, String info) {
}
