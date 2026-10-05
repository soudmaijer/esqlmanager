package nl.errorsoft.esql.connection.ui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerType;

/** Shows a profile, a server type or a connection window with the brand icon of its server, another internal frame with its frame icon. */
public class ServerIconRenderer extends DefaultListCellRenderer {
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		ServerType type = switch (value) {
			case ConnectionProfile profile -> profile.getServerType();
			case ServerType serverType -> serverType;
			case ConnectionWindowUI window -> window.getControlClass().getConnectionProfile().getServerType();
			case null, default -> null;
		};
		if (type == null && value instanceof javax.swing.JInternalFrame frame) {
			// A window without a server of its own, such as the designer, shows its frame icon.
			setIcon(frame.getFrameIcon());
			return this;
		}
		setIcon(type == null ? null : ApplicationContext.get().imageLoader().getIcon(isSelected ? type.iconName() + "sel" : type.iconName()));
		return this;
	}
}
