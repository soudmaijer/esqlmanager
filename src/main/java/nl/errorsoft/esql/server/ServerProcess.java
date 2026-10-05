package nl.errorsoft.esql.server;

/**
 * A connection that is active on the server, as shown in the process list.
 *
 * @param time seconds the process has been in its current state
 * @param idle true when the process waits for its client and runs nothing, so the list can hide it
 */
public record ServerProcess(String id, String user, String host, String database, String command, String time, String info, boolean idle) {
}
