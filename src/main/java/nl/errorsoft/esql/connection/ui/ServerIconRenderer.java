package nl.errorsoft.esql.connection.ui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerType;

/** Shows a profile or a server type with the brand icon of its server. */
class ServerIconRenderer extends DefaultListCellRenderer {
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		ServerType type = switch (value) {
			case ConnectionProfile profile -> profile.getServerType();
			case ServerType serverType -> serverType;
			case null, default -> null;
		};
		setIcon(type == null ? null : ApplicationContext.get().imageLoader().getIcon(type.iconName()));
		return this;
	}
}
