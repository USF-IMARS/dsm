/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import java.io.File;
import com.enterprisedt.net.ftp.*;

/**
 * FtpSender sends files to a host using ftp.
 */
    class FtpSender
    {
        private String site;
        private FTPClient ftp;

        FtpSender(String site, String host, boolean isPassive, String user,
                String paswd) throws Exception
        {
            this.site = site;
            ftp = new FTPClient();
            ftp.setRemoteHost(host);
            ftp.connect();
            ftp.setTimeout(100 * 1000);  //100 seconds for now
            ftp.login(user,paswd);
            ftp.setType(FTPTransferType.BINARY);
            ftp.setConnectMode(isPassive? FTPConnectMode.PASV : FTPConnectMode.ACTIVE);
        }

        final String getSite()
        {
            return site;
        }

        /**
         * Put pds into the remote ftp directory.
         */
        void sendPDS(PDS pds, File remoteDirectory) throws Exception
        {
            File absoluteRemoteDirectory = remoteDirectory;
            if (!remoteDirectory.isAbsolute())
            {
                absoluteRemoteDirectory = new File("/",remoteDirectory.getPath());
            }
            ftp.chdir(absoluteRemoteDirectory.getPath());
            File xdata = pds.getDataFile();
            File xcrec = pds.getRecordFile();
            ftp.put(xdata.getAbsolutePath(),xdata.getName());
            ftp.put(xcrec.getAbsolutePath(),xcrec.getName());
        }

        /**
         * Put file into the remote ftp directory.
         */
        void sendFile(File file, File remoteDirectory) throws Exception
        {
            File absoluteRemoteDirectory = remoteDirectory;
            if (!remoteDirectory.isAbsolute())
            {
                absoluteRemoteDirectory = new File("/",remoteDirectory.getPath());
            }
            ftp.chdir(absoluteRemoteDirectory.getPath());
            ftp.put(file.getAbsolutePath(),file.getName());
        }

        void quit() throws Exception
        {
            ftp.quit();
        }
    }
