package com.indexdata.masterkey.localindices.harvest.storage.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.log4j.Level;

import com.indexdata.masterkey.localindices.util.TextUtils;

public class ZebraStorageBackendServletContext extends ZebraStorageBackend {
  private ServletContext ctx;

  public ZebraStorageBackendServletContext(String file_url, String idxName, ServletContext ctx) {
    super(file_url, idxName);
    this.ctx = ctx;
  }

  @Override
  public void init(Properties props) {
    super.init(props);
    unpackDir(ctx, "/WEB-INF/stylesheets", baseDirectory.getAbsolutePath() + "/stylesheets");
    unpackDir(ctx, "/WEB-INF/zebra_dom_conf", baseDirectory.getAbsolutePath());
    String[] tokens = { "CONFIG_DIR", baseDirectory.getAbsolutePath(), "HARVEST_DIR",
	new File(baseDirectory, indexName).getAbsolutePath() };
    unpackResourceWithSubstitute(ctx, "/WEB-INF/zebra.cfg", baseDirectory + "/zebra.cfg", tokens);
    // TODO Verify this will both come in standard and reindex
    unpackResourceWithSubstitute(ctx, "/WEB-INF/reindex.rb", baseDirectory + "/reindex.rb", null);
    unpackResourceWithSubstitute(ctx, "/WEB-INF/addlexis.rb", baseDirectory + "/addlexis.rb", null);
  }

  /**
   * 
   * @param ctx
   * @param source
   *          full source path (includinf file name)
   * @param dest
   *          full destination path (without the file name)
   */
  private void unpackDir(ServletContext ctx, String source, String dest) {
    // first check if the target directory exists, if not create it
    File destDir = new File(dest);
    if (!destDir.exists())
      destDir.mkdirs();
    // get all subfiles from the source and copy them over
    for (Object resource : ctx.getResourcePaths(source)) {
      String resourcePath = (String) resource;
      try {
	InputStream is = ctx.getResourceAsStream(resourcePath);
	String fileName = resourcePath.substring(resourcePath.lastIndexOf("/"));
	FileOutputStream os = new FileOutputStream(dest + "/" + fileName);
	TextUtils.copyStream(is, os);
	os.close();
	is.close();
      } catch (IOException ioe) {
	logger.log(Level.WARN, "Cannot unpack file " + resourcePath + " to " + dest);
      }
    }
  }

  private void unpackResourceWithSubstitute(ServletContext ctx, String source, String dest,
      String[] tokens) {
    File destFile = new File(dest.substring(0, dest.lastIndexOf('/')));
    if (!destFile.exists())
      destFile.mkdirs();
    try {
      InputStream is = ctx.getResourceAsStream(source);
      FileOutputStream os = new FileOutputStream(dest);
      TextUtils.copyStreamWithReplace(is, os, tokens);
      os.close();
      is.close();
    } catch (IOException ioe) {
      logger.log(Level.WARN, "Cannot unpack resource " + source + " to " + dest);
    }
  }

}
