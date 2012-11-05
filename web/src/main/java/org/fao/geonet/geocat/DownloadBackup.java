package org.fao.geonet.geocat;

import java.io.File;

import jeeves.exceptions.JeevesException;
import jeeves.interfaces.Service;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import jeeves.utils.BinaryFile;
import jeeves.utils.Log;

import org.fao.geonet.GeonetworkDataDirectory;
<<<<<<< HEAD
import org.fao.geonet.constants.Geonet;
=======
>>>>>>> origin/geocat_stable
import org.jdom.Element;

public class DownloadBackup implements Service {

	@Override
	public void init(String appPath, ServiceConfig params) throws Exception {
	}

	@Override
	public Element exec(Element params, ServiceContext context)
			throws Exception {
<<<<<<< HEAD
		Log.info(Geonet.GEONETWORK, "User "+context.getUserSession().getUsername()+" from IP: "+context.getIpAddress()+" has started to download backup archive");
=======
		Log.info(ArchiveAllMetadataJob.BACKUP_LOG, context.getIpAddress()+" has started to download backup archive");
>>>>>>> origin/geocat_stable
		String datadir = System.getProperty(GeonetworkDataDirectory.GEONETWORK_DIR_KEY);
		File backupDir = new File(datadir, ArchiveAllMetadataJob.BACKUP_DIR);
		if (!backupDir.exists()) {
			throw404();
		}
		File[] files = backupDir.listFiles();
		if(files == null || files.length == 0) {
			throw404();
		}
		return BinaryFile.encode(200, files[0].getPath(), false);
	}

<<<<<<< HEAD
	private void throw404() {
=======
	private void throw404() throws JeevesException {
>>>>>>> origin/geocat_stable
		throw new JeevesException("Backup file does not yet exist", null) {
			private static final long serialVersionUID = 1L;
			{
				this.code = 404;
<<<<<<< HEAD
=======
				this.id = "NoBackup";
>>>>>>> origin/geocat_stable
			}
		};
	}

}
