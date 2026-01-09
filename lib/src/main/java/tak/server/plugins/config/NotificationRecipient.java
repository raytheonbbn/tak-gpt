package tak.server.plugins.config;

import java.io.File;

public interface NotificationRecipient {
	public void fileChangeOccurred(File file);
}
