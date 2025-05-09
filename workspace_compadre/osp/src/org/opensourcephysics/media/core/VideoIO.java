/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2004  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.ResourceLoader;
import org.opensourcephysics.tools.ExtensionsManager;

/**
 * This provides static methods for managing video and text input/output.
 *
 * @author Douglas Brown
 * @version 1.0
 */
@SuppressWarnings("unchecked")
public class VideoIO {
	
  // static constants
	@SuppressWarnings("javadoc")
	public static final String[] VIDEO_EXTENSIONS = {"mov", "avi", "mp4"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	// static fields
  protected static JFileChooser chooser;
  protected static VideoFileFilter videoFileFilter = new VideoFileFilter();
	protected static Collection<VideoFileFilter> singleVideoTypeFilters 
			= new TreeSet<VideoFileFilter>();
  protected static FileFilter imageFileFilter;
  protected static ArrayList<VideoType> videoTypes = new ArrayList<VideoType>();
  protected static ArrayList<VideoType> videoEngines = new ArrayList<VideoType>();
  protected static String defaultXMLExt = "xml"; //$NON-NLS-1$
  @SuppressWarnings("javadoc")
	public static boolean loadQTLast = true;
  protected static VideoEnginePanel videoEnginePanel;
  protected static boolean canceled;

  static {
    // add video types
    // add Gif video type, if available
    try {
      String name = "org.opensourcephysics.media.gif.GifVideoType"; //$NON-NLS-1$
      Class<VideoType> gifClass = (Class<VideoType>)Class.forName(name);
      addVideoType(gifClass.newInstance());
    } catch(Exception ex) {
    } catch(Error err) {
    }
    VideoFileFilter filter = new VideoFileFilter("jpg",  //$NON-NLS-1$
    		new String[] {"jpg", "jpeg"}); //$NON-NLS-1$ //$NON-NLS-2$
    addVideoType(new ImageVideoType(filter));
    filter = new VideoFileFilter("png", new String[] {"png"}); //$NON-NLS-1$ //$NON-NLS-2$
    addVideoType(new ImageVideoType(filter));
    imageFileFilter = new FileFilter() {
      public boolean accept(File f) {
        if(f==null) {
          return false;
        }
        if(f.isDirectory()) {
          return true;
        }
        String extension = VideoIO.getExtension(f);
        if((extension!=null)&&(extension.equals("gif")|| //$NON-NLS-1$
          extension.equals("jpg"))) {//$NON-NLS-1$
          return true;                                   
        }
        return false;
      }
      public String getDescription() {
        return MediaRes.getString("VideoIO.ImageFileFilter.Description");//$NON-NLS-1$
      } 

    };
    // video engine panel
    videoEnginePanel = new VideoEnginePanel();
  }

  /**
   * protected constructor to discourage instantiation
   */
  protected VideoIO() {

  /** empty block */
  }

  /**
   * Gets the extension of a file.
   *
   * @param file the file
   * @return the extension of the file
   */
  public static String getExtension(File file) {
    return XML.getExtension(file.getName());
  }

  /**
   * Gets the file chooser.
   *
   * @return the file chooser
   */
  public static JFileChooser getChooser() {
    if(chooser==null) {
    	File dir = (OSPRuntime.chooserDir==null)? 
    			new File(System.getProperty("user.home")):  //$NON-NLS-1$
    			new File(OSPRuntime.chooserDir);
      chooser = new JFileChooser(dir);
      chooser.addPropertyChangeListener(videoEnginePanel);
    }
    return chooser;
  }

  /**
   * Sets the default xml extension used when saving data.
   *
   * @param ext the default extension
   */
  public static void setDefaultXMLExtension(String ext) {
    defaultXMLExt = ext;
  }

  /**
   * Gets the path relative to the user directory.
   *
   * @param absolutePath the absolute path
   * @return the relative path
   */
  public static String getRelativePath(String absolutePath) {
    if((absolutePath.indexOf("/")==-1)&&(absolutePath.indexOf("\\")==-1)) { //$NON-NLS-1$ //$NON-NLS-2$
      return absolutePath;
    }
    if(absolutePath.startsWith("http:")) {//$NON-NLS-1$
      return absolutePath; 
    }
    String path = absolutePath;
    String relativePath = "";                     //$NON-NLS-1$
    boolean validPath = false;
    // relative to user directory
    String base = System.getProperty("user.dir"); //$NON-NLS-1$
    if(base==null) {
      return path;
    }
    for(int j = 0; j<3; j++) {
      if(j>0) {
        // move up one level
        int k = base.lastIndexOf("\\");                        //$NON-NLS-1$
        if(k==-1) {
          k = base.lastIndexOf("/");                           //$NON-NLS-1$
        }
        if(k!=-1) {
          base = base.substring(0, k);
          relativePath += "../";                               //$NON-NLS-1$
        } else {
          break;                                               // no more levels!
        }
      }
      if(path.startsWith(base)) {
        path = path.substring(base.length()+1);
        // replace backslashes with forward slashes
        int i = path.indexOf("\\");                            //$NON-NLS-1$
        while(i!=-1) {
          path = path.substring(0, i)+"/"+path.substring(i+1); //$NON-NLS-1$
          i = path.indexOf("\\");                              //$NON-NLS-1$
        }
        relativePath += path;
        validPath = true;
        break;
      }
    }
    if(validPath) {
      return relativePath;
    }
    return path;
  }

  /**
   * Updates a VideoType by copying files or creating symlinks if needed.
   *
   * @param type the VideoType class
   * @return true if updated
   */
  public static boolean updateVideoType(Class<? extends VideoType> type) {
    // set up java vm extensions folders
    String extFolders = XML.forwardSlash(System.getProperty("java.ext.dirs")); //$NON-NLS-1$
    ArrayList<File> extDirs = new ArrayList<File>();
    String separator = System.getProperty("path.separator"); //$NON-NLS-1$
    int n = extFolders.indexOf(separator);
    while (n>-1) {
    	extDirs.add(new File(extFolders.substring(0, n)));
    	extFolders = extFolders.substring(n+1);
      n = extFolders.indexOf(separator);
    }    
    if (!"".equals(extFolders)) //$NON-NLS-1$
    	extDirs.add(new File(extFolders));
    
  	ExtensionsManager jarManager = new ExtensionsManager();
  	if (type.getSimpleName().equals("XuggleVideoType")) { //$NON-NLS-1$
  		boolean copied = false;
  		for (File extDir: extDirs) {
  			if (!extDir.exists()) continue;
  			copied = jarManager.copyXuggleJarsTo(extDir, false) || copied;
  		}
  		return copied;
  	}
  	else if (type.getSimpleName().equals("QTVideoType")) { //$NON-NLS-1$
    	if (org.opensourcephysics.display.OSPRuntime.isLinux()) return false;
  		boolean copied = false;
  		for (File extDir: extDirs) {
  			if (!extDir.exists()) continue;
  			copied = jarManager.copyQTJavaTo(extDir, false) || copied;
  		}
  		return copied;
  	}
    return false;
  }
  
  /**
   * test executing shell commands
   */
  public static void testExec() {
//  	System.getProperties().list(System.out);
//    // get java vm extensions folder
//    String extFolder = XML.forwardSlash(System.getProperty("java.ext.dirs")); //$NON-NLS-1$
//    // keep only first folder listed
//    String sep = System.getProperty("path.separator");
//    if (extFolder.indexOf(sep) > -1) {
//    	extFolder = extFolder.substring(0, extFolder.indexOf(sep));
//    }
//    extFolder = extFolder+"/"; //$NON-NLS-1$
//    // get xuggle folder and jar names
//    String xuggleHome = System.getenv("XUGGLE_HOME"); //$NON-NLS-1$
//  	String xuggleFolder = XML.forwardSlash(xuggleHome)+"/share/java/jars/"; //$NON-NLS-1$
//    String[] jarNames = {"xuggle-xuggler.jar","slf4j-api.jar", //$NON-NLS-1$ //$NON-NLS-2$
//    		"logback-core.jar","logback-classic.jar"}; //$NON-NLS-1$ //$NON-NLS-2$
//  	String shellCmd = "#!/bin/bash\nsudo cp "+xuggleFolder+"xuggle-xuggler.jar "+extFolder+"xuggle-copy.jar";        
//  	shellCmd = "#!/bin/bash\ncp "+xuggleFolder+"xuggle-xuggler.jar ~/junk.jar";        
//  	String home = System.getProperty("user.home");
//  	String fileName = home+"/copyXuggle.sh";
//    try {
//      File file = new File(fileName);
//      FileOutputStream stream = new FileOutputStream(file);
//      java.nio.charset.Charset charset = java.nio.charset.Charset.forName("UTF-8"); //$NON-NLS-1$
//      Writer out = new OutputStreamWriter(stream, charset);
//      Writer output = new BufferedWriter(out);
//      output.write(shellCmd);
//      output.flush();
//      output.close();
//      Runtime.getRuntime().exec("chmod +x "+fileName);
//      
//      // open a terminal and write to it
//      String[] cmd = {"gnome-terminal", "cd ~/Tracker\n"};
//      Process process = Runtime.getRuntime().exec(cmd);
//      new Thread(new StreamPiper(process.getErrorStream(), System.err)).start();
//      new Thread(new StreamPiper(process.getInputStream(), System.out)).start();
////      Writer stdin = new OutputStreamWriter(process.getOutputStream());
////      stdin.write(shellCmd);
////      stdin.write("gnome-terminal&\n");
////      stdin.write("xterm&\n");
////      stdin.write("cd ~/Tracker\n");
////      stdin.write("dir\n");
////      stdin.write("ls\n");
////      stdin.close();
//
//      final int exitVal = process.waitFor();
//      System.out.println("Exit value: " + exitVal);
//    } catch(Exception ex) {
//      ex.printStackTrace();
//    }
//      	
  }
  
  /**
   */
  public static class StreamPiper implements Runnable {

    private final InputStream input;
    private final OutputStream output;

    /**
     * @param in
     * @param out
     */
    public StreamPiper(InputStream in, OutputStream out) {
      input = in;
      output = out;
    }
   
    public void run() {
      try {
        final byte[] buffer = new byte[1024];
        for (int count = 0; (count = input.read(buffer)) >= 0;) {
          output.write(buffer, 0, count);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }   
  }
  
  /**
   * Copies a source file to a target file.
   *
   * @param inFile the source
   * @param outFile the target
   * @return true if successfully copied
   */
  public static boolean copyFile(File inFile, File outFile) {
  	byte[] buffer = new byte[100000];
    try {
    	InputStream in = new FileInputStream(inFile);
    	OutputStream out = new FileOutputStream(outFile);
			while (true) {
				synchronized (buffer) {
					int amountRead = in.read(buffer);
					if (amountRead == -1) {
						break;
					}
					out.write(buffer, 0, amountRead);
				}
			}
			in.close();
			out.close();
			outFile.setLastModified(inFile.lastModified());
		}                   
    catch (IOException ex) {
    	return false;
    }
  	return true;
  }

  /**
   * Returns the currently supported video file extensions
   *
   * @return an array of extensions
   */
  public static String[] getVideoExtensions() {
  	return videoFileFilter.getExtensions();
  }

  /**
   * Adds a video type to the list of available types
   *
   * @param type the video type
   */
  public static void addVideoType(VideoType type) {
    if(type!=null) {
      boolean hasType = false;
      for (VideoType next: videoTypes) {
        if(next.getDescription().equals(type.getDescription())
        		&& next.getClass()==type.getClass()) {
          hasType = true;
        }
      }
      if(!hasType) {
        videoTypes.add(type);
        if (getVideoType("qt", null)!=null) //$NON-NLS-1$
        	setQTLast(loadQTLast);
        VideoFileFilter filter = type.getDefaultFileFilter();
        if (filter!=null && filter.extensions!=null) {
        	singleVideoTypeFilters.add(filter);
        }
      }
    }
  }

  /**
   * Adds a video engine to the list of available types
   *
   * @param type the video type
   */
  public static void addVideoEngine(VideoType type) {
    if(type!=null) {
    	OSPLog.finest(type.getClass().getSimpleName()+" "+type.getDefaultExtension()); //$NON-NLS-1$
      boolean hasType = false;
      for (VideoType next: videoEngines) {
        if(next.getClass()==type.getClass()) {
          hasType = true;
        }
      }
      if(!hasType) {
      	videoEngines.add(type);
      	videoEnginePanel.addVideoEngine(type);
      }
    }
  }

  /**
   * Returns the first registered video type corresponding to a class name 
   * and/or extension. Strings are case-insensitive.
   *
   * @param className all or part of the simple class name (may be null)
   * @param extension the extension (may be null)
   * @return the video type
   */
  public static VideoType getVideoType(String className, String extension) {
  	if (className==null && extension==null)
  		return null;
  	ArrayList<VideoType> candidates = new ArrayList<VideoType>();
  	synchronized(videoTypes) {
	  	// first pass: check class names
	  	if (className==null) {
	    	candidates.addAll(videoTypes);
	  	}
	  	else {
	    	className = className.toLowerCase();
	      for (VideoType next: videoTypes) {
	      	String name = next.getClass().getSimpleName().toLowerCase();
	      	if (name.indexOf(className)>-1)
	      		candidates.add(next);
	      }	
	  	}  	
	  	if (extension==null) {
	  		if (candidates.isEmpty())
	  			return null;
	  		return candidates.get(0);
	  	}
	  	// second pass: compare with default extension
			extension = extension.toLowerCase();
	    for (VideoType next: candidates) {
	    	String id = next.getDefaultExtension();
	    	if (id!=null && id.indexOf(extension)>-1)
	    		return next;
	    }	
	    // third pass: compare with all extensions
	    for (VideoType next: candidates) {
	    	VideoFileFilter[] filters = next.getFileFilters();
	    	for (VideoFileFilter filter: filters) {
	    		if (filter.extensions!=null) {
	    			for (String s: filter.extensions)
	    				if (s.indexOf(extension)>-1)
	    					return next;
	    		}
	    	}
	    }
  	}
    return null;
  }

  /**
   * Gets an array of video types that can open files with a given extension
   *
   * @param ext the extension
   * @return the video types
   */
  public static VideoType[] getVideoTypes(String ext) {
  	ext = ext.toLowerCase();
  	ArrayList<VideoType> found = new ArrayList<VideoType>();
  	// first add types for which ext is the default extension
    for (VideoType next: videoTypes) {
    	String id = next.getDefaultExtension();
    	if (id!=null && id.indexOf(ext)>-1)
    		found.add(next);
    }
    // then add types for which ext is accepted
    for (VideoType next: videoTypes) {
    	VideoFileFilter[] filters = next.getFileFilters();
    	for (VideoFileFilter filter: filters) {
    		if (filter.extensions!=null) {
    			for (String s: filter.extensions)
    				if (s.indexOf(ext)>-1 && !found.contains(next))
    	    		found.add(next);
    		}
    	}
    }	
    return found.toArray(new VideoType[0]);
  }

  /**
   * Gets an array of available video types
   *
   * @return the video types
   */
  public static VideoType[] getVideoTypes() {
    return videoTypes.toArray(new VideoType[0]);
  }

  /**
   * Cancels the current operation when true.
   *
   * @param cancel true to cancel
   */
  public static void setCanceled(boolean cancel) {
  	canceled = cancel;
  }
  
  /**
   * Determines if the current operation is canceled.
   *
   * @return true if canceled
   */
  public static boolean isCanceled() {
  	return canceled;
  }
  
  /**
   * Sets the loadQTLast flag. When true, QTVideo is used only if all 
   * others fail to load a video.
   *
   * @param last true to load QTVideo last
   */
  public static void setQTLast(boolean last) {
    loadQTLast = last;    
    if (last) {
    	// move QTVideoType (if present) to end
      for (int i = 0; i < videoTypes.size(); i++) {
      	VideoType next = videoTypes.get(i);
      	if (next.getClass().getSimpleName().equals("QTVideoType") //$NON-NLS-1$
      			&& i < videoTypes.size()-1) {
      		videoTypes.remove(next);
      		videoTypes.add(next); // put at end
      		break;
      	}
      }
    }
    else {
    	// move QTVideoType (if present) before xuggle (if present)
    	int qtIndex = -1, xuggleIndex = -1;
      for (int i = 0; i < videoTypes.size(); i++) {
      	VideoType next = videoTypes.get(i);
      	if (next.getClass().getSimpleName().equals("QTVideoType")) { //$NON-NLS-1$
      		qtIndex = i;
      	}
      	else if (next.getClass().getSimpleName().equals("XuggleVideoType") //$NON-NLS-1$
      			&& xuggleIndex==-1) {
      		xuggleIndex = i; // first xuggle type
      	}
      }
      if (qtIndex>-1 && xuggleIndex>-1 && qtIndex>xuggleIndex) {
      	VideoType qtType = videoTypes.remove(qtIndex);
      	videoTypes.add(xuggleIndex, qtType);
      }
    }
  }

  /**
   * Returns true if QTVideo is present and preferred.
   *
   * @return true if QTVideo is preferred
   */
  public static boolean isQTPreferred() {
  	if (getVideoType("qt", null)==null) return false; // QT is unavailable //$NON-NLS-1$
  	if (getVideoType("xuggle", null)==null) return true; // QT is only type available //$NON-NLS-1$
  	return !loadQTLast; // QT is not last
  }

  /**
   * Returns a video from a specified path. May return null.
   *
   * @param path the path
   * @param vidType a requested video type (may be null)
   * @return the video
   */
  public static Video getVideo(String path, VideoType vidType) {
  	OSPLog.fine("path: "+path+" type: "+vidType); //$NON-NLS-1$ //$NON-NLS-2$
    Video video = null;
		VideoIO.setCanceled(false);
		
    // try first with specified VideoType, if any
    if (vidType!=null) {
    	OSPLog.finest("requested type "+vidType.getClass().getSimpleName() //$NON-NLS-1$
    			+" "+vidType.getDescription()); //$NON-NLS-1$
      video = vidType.getVideo(path);
    }  
		if (VideoIO.isCanceled()) return null;
		
    // get all VideoTypes for the file extension
    if (video==null) {
    	String extension = XML.getExtension(path);
      VideoType[] types = getVideoTypes(extension);
      ArrayList<VideoType> preferredTypes = new ArrayList<VideoType>();
      // first try with preferred type if available
      for(int i = 0; i<types.length; i++) {
      	boolean isQT = types[i].getClass().getSimpleName().indexOf("QT")>-1; //$NON-NLS-1$
      	if ((isQT && isQTPreferred()) || (!isQT && !isQTPreferred())) {
        	preferredTypes.add(types[i]);
      	}
      }
      for (VideoType next: preferredTypes) {
      	OSPLog.finest("preferred type "+next.getClass().getSimpleName() //$NON-NLS-1$
      			+" "+next.getDescription()); //$NON-NLS-1$
      	video = next.getVideo(path);
	  		if (VideoIO.isCanceled()) return null;
      	if (video!=null)  return video;      	
      }
      // if preferred type not found or unsuccessful, try remaining types
      for(int i = 0; i<types.length; i++) {
      	if (preferredTypes.contains(types[i])) continue;
      	OSPLog.finest("type "+types[i].getClass().getSimpleName() //$NON-NLS-1$
      			+" "+types[i].getDescription()); //$NON-NLS-1$
      	video = types[i].getVideo(path);
    		if (VideoIO.isCanceled()) return null;
      	if (video!=null) return video;
      }
    }
    return video;
  }

  /**
   * Returns a clone of the specified video.
   *
   * @param video the video to clone
   * @return the clone
   */
  public static Video clone(Video video) {
    if(video==null) {
      return null;
    }
    // ImageVideo is special case since may have pasted images
    if(video instanceof ImageVideo) {
      ImageVideo oldVid = (ImageVideo) video;
      ImageVideo newVid = new ImageVideo(oldVid.getImages());
      newVid.rawImage = newVid.images[0];
      Collection<Filter> filters = video.getFilterStack().getFilters();
      if(filters!=null) {
        Iterator<Filter> it = filters.iterator();
        while(it.hasNext()) {
          Filter filter = it.next();
          newVid.getFilterStack().addFilter(filter);
        }
      }
      return newVid;
    }
    XMLControl control = new XMLControlElement(video);
    return(Video) new XMLControlElement(control).loadObject(null);
  }

  /**
   * Loads the specified video panel from a file selected with a chooser
   * and sets the data file of the panel.
   *
   * @param vidPanel the video panel
   * @return an array containing the loaded object and file
   */
  public static File open(VideoPanel vidPanel) {
    return open((File) null, vidPanel);
  }

  /**
   * Displays a file chooser and returns the chosen files.
   *
   * @param type may be "open", "open video", "save", "insert image"
   * @return the files, or null if no files chosen
   */
  public static File[] getChooserFiles(String type) {
    JFileChooser chooser = getChooser();
    chooser.setMultiSelectionEnabled(false);
    chooser.setAcceptAllFileFilterUsed(true);
    int result = JFileChooser.CANCEL_OPTION;
    if(type.toLowerCase().equals("open")) { // open any file //$NON-NLS-1$
      chooser.addChoosableFileFilter(videoFileFilter);
      chooser.setFileFilter(chooser.getAcceptAllFileFilter());
      result = chooser.showOpenDialog(null);
    } 
    else if(type.toLowerCase().equals("open video")) { // open video //$NON-NLS-1$
      chooser.addChoosableFileFilter(videoFileFilter);
      result = chooser.showOpenDialog(null);
    } 
    else if(type.toLowerCase().equals("save")) { // save any file //$NON-NLS-1$
    	// note this sets no file filters but does include acceptAll
      // also sets file name to "untitled"
      String filename = MediaRes.getString("VideoIO.FileName.Untitled"); //$NON-NLS-1$
	    chooser.setSelectedFile(new File(filename+"."+defaultXMLExt)); //$NON-NLS-1$
      result = chooser.showSaveDialog(null);
    } 
    else if(type.toLowerCase().equals("insert image")) { //$NON-NLS-1$
      chooser.setMultiSelectionEnabled(true);
      chooser.setAcceptAllFileFilterUsed(false);
      chooser.addChoosableFileFilter(imageFileFilter);
      chooser.setSelectedFile(new File("")); //$NON-NLS-1$
      result = chooser.showOpenDialog(null);
      File[] files = chooser.getSelectedFiles();
      chooser.removeChoosableFileFilter(imageFileFilter);
      chooser.setSelectedFile(new File(""));  //$NON-NLS-1$
      if(result==JFileChooser.APPROVE_OPTION) {
        return files;
      }
    }
    if(result==JFileChooser.APPROVE_OPTION) {
    	File file = chooser.getSelectedFile();
      chooser.removeChoosableFileFilter(videoFileFilter);
      chooser.setSelectedFile(new File(""));  //$NON-NLS-1$
      return new File[] {file};
    }
    return null;
  }

  /**
   * Loads data or a video from a specified file into a VideoPanel.
   * If file is null, a file chooser is displayed.
   *
   * @param file the file to be loaded
   * @param vidPanel the video panel
   * @return the file opened
   */
  public static File open(File file, VideoPanel vidPanel) {
    if(file==null) {
      File[] files = getChooserFiles("open");                                  //$NON-NLS-1$
      if(files!=null) {
        file = files[0];
      }
    }
    if(file==null) {
      return null;
    }
    if(videoFileFilter.accept(file)) {                                                             // load video
      VideoType[] types = getVideoTypes();
      Video video = null;            
      for(int i = 0; i<types.length; i++) {
        video = types[i].getVideo(file.getAbsolutePath());
        if(video!=null) {
        	OSPLog.info(file.getName()+" opened as type "+types[i].getDescription()); //$NON-NLS-1$
          break;
        }
        OSPLog.info(file.getName()+" failed as type "+types[i].getDescription()); //$NON-NLS-1$
      }
      if(video!=null) {
        vidPanel.setVideo(video);
        vidPanel.repaint();
      } else {
        JOptionPane.showMessageDialog(vidPanel, MediaRes.getString("VideoIO.Dialog.BadVideo.Message")+ //$NON-NLS-1$
        		ResourceLoader.getNonURIPath(XML.getAbsolutePath(file)));
      }
    } 
    else {                                                                                       // load data
      XMLControlElement control = new XMLControlElement();
      control.read(file.getAbsolutePath());
      Class<?> type = control.getObjectClass();
      if(VideoPanel.class.isAssignableFrom(type)) {
        vidPanel.setDataFile(file);
        control.loadObject(vidPanel);
      } else if(!control.failedToRead()) {
        JOptionPane.showMessageDialog(vidPanel, "\""+file.getName()+"\" "+                             //$NON-NLS-1$ //$NON-NLS-2$
          MediaRes.getString("VideoIO.Dialog.XMLMismatch.Message"),                                //$NON-NLS-1$
            MediaRes.getString("VideoIO.Dialog.XMLMismatch.Title"), JOptionPane.WARNING_MESSAGE);  //$NON-NLS-1$
        return null;
      } else {
        JOptionPane.showMessageDialog(vidPanel, MediaRes.getString("VideoIO.Dialog.BadFile.Message")+  //$NON-NLS-1$
        		ResourceLoader.getNonURIPath(XML.getAbsolutePath(file)));
      }
      vidPanel.changed = false;
    }
    return file;
  }

  /**
   * Writes VideoPanel data to the specified file. If the file is null
   * it brings up a chooser.
   *
   * @param file the file to write to
   * @param vidPanel the video panel
   * @return the file written to, or null if not written
   */
  public static File save(File file, VideoPanel vidPanel) {
    return save(file, vidPanel, MediaRes.getString("VideoIO.Dialog.SaveAs.Title"));        //$NON-NLS-1$
  }
  
  
  /**
   * Writes VideoPanel data to the specified file. If the file is null it displays a filechooser.
   *
   * @param file the file to write to
   * @param vidPanel the video panel
   * @param chooserTitle the title for the filechooser
   * @return the file written to, or null if not written
   */
  public static File save(File file, VideoPanel vidPanel, String chooserTitle) {
    if(file==null) {
      Video video = vidPanel.getVideo();
      JFileChooser chooser = getChooser();
      chooser.removeChoosableFileFilter(videoFileFilter);
      chooser.removeChoosableFileFilter(imageFileFilter);
      chooser.setDialogTitle(chooserTitle);
      String filename = MediaRes.getString("VideoIO.FileName.Untitled");                //$NON-NLS-1$
      if(vidPanel.getFilePath()!=null) {
        filename = XML.stripExtension(vidPanel.getFilePath());
      } 
      else if((video!=null)&&(video.getProperty("name")!=null)) {                     //$NON-NLS-1$
        filename = (String) video.getProperty("name");                                  //$NON-NLS-1$
        int i = filename.lastIndexOf(".");                                              //$NON-NLS-1$
        if(i>0) {
          filename = filename.substring(0, i);
        }
      }
      file = new File(filename+"."+defaultXMLExt);                                      //$NON-NLS-1$
      String parent = XML.getDirectoryPath(filename);
      if(!parent.equals("")) {                                                          //$NON-NLS-1$
        XML.createFolders(parent);
        chooser.setCurrentDirectory(new File(parent));
      }
      chooser.setSelectedFile(file);
      int result = chooser.showSaveDialog(vidPanel);
      if(result==JFileChooser.APPROVE_OPTION) {
        file = chooser.getSelectedFile();
        if(!defaultXMLExt.equals(getExtension(file))) {
          filename = XML.stripExtension(file.getPath());
          file = new File(filename+"."+defaultXMLExt);                                  //$NON-NLS-1$
        }
        if(file.exists()) {
          int selected = JOptionPane.showConfirmDialog(vidPanel, " \""+file.getName()+"\" " //$NON-NLS-1$ //$NON-NLS-2$
            +MediaRes.getString("VideoIO.Dialog.FileExists.Message"),                   //$NON-NLS-1$
              MediaRes.getString("VideoIO.Dialog.FileExists.Title"),                    //$NON-NLS-1$
                JOptionPane.OK_CANCEL_OPTION);
          if(selected!=JOptionPane.OK_OPTION) {
            return null;
          }
        }
        vidPanel.setDataFile(file);
      } else {
        return null;
      }
    }
    Video video = vidPanel.getVideo();
    if(video!=null) {
      video.setProperty("base", XML.getDirectoryPath(XML.getAbsolutePath(file))); //$NON-NLS-1$
      if(video instanceof ImageVideo) {
        ((ImageVideo) video).saveInvalidImages();
      }
    }
    XMLControl xmlControl = new XMLControlElement(vidPanel);
    xmlControl.write(file.getAbsolutePath());
    vidPanel.changed = false;
    return file;
  }
  
  /**
   * Writes an image to a file.
   * @param image the image to write
   * @param filePath the path to write to, including extension (png, jpg, gif)
   * @return the file written, or null if failed
   */  
	public static File writeImageFile(BufferedImage image, String filePath) {
		if (image==null) return null;
		File file = new File(filePath);
		File parent = file.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}
		String ext = XML.getExtension(filePath);
	 	try {
			if (ImageIO.write(image, ext, file))
				return file;
		} catch (IOException ex) {
			OSPLog.finer(ex.toString());
		}
	  return null;
	}
	 
  /**
   * A JPanel for setting a preferred video engine when opening a video.
   */  
  protected static class VideoEnginePanel extends JPanel implements PropertyChangeListener {
  	
  	JPanel emptyPanel;
  	File selectedFile;
  	ButtonGroup videoEngineButtonGroup = new ButtonGroup();
  	HashMap<JRadioButton, VideoType> buttonMap = new HashMap<JRadioButton, VideoType>();
  	TitledBorder title;
  	boolean isClosing = false;

    /**
     * Constructor
     */  
  	VideoEnginePanel() {
  		super();
  		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  		title = BorderFactory.createTitledBorder(MediaRes.getString("VideoEnginePanel.TitledBorder.Title")); //$NON-NLS-1$
  		setBorder(title);
  		emptyPanel = new JPanel() {
  			public Dimension getPreferredSize() {
  				return videoEnginePanel.getPreferredSize();
  			}
  		};
  	}
  	
    /**
     * Adds a video engine type to the available choices.
     * 
     * @param type the video engine type
     */  
  	public void addVideoEngine(VideoType type) {
  		JRadioButton button = new JRadioButton(type.getDescription());
  		button.setActionCommand(type.getClass().getSimpleName());
  		videoEngineButtonGroup.add(button);
  		buttonMap.put(button, type);
  		add(button);  		
  	}
  	
    /**
     * Gets the selected video engine type.
     * 
     * @return the video engine type
     */  
  	public VideoType getSelectedVideoType() {
  		if (chooser.getAccessory()==null
  				|| chooser.getAccessory()==emptyPanel)
  			return null;
      for (JRadioButton button: buttonMap.keySet()) {
      	if (button.isSelected()) {
      		VideoType engineType = buttonMap.get(button);
      		OSPLog.finest("selected video type: "+engineType); //$NON-NLS-1$
      		String engineName = engineType.getClass().getSimpleName();
      		String ext = XML.getExtension(selectedFile.getName());
      		VideoType specificType = getVideoType(engineName, ext);
      		return specificType==null? engineType: specificType;
      	}
      }
      return null;
  	}
  	
    /**
     * Resets to a ready state.
     */  
  	public void reset() {
  		isClosing = false;
  		refresh();
  	}
  	
    /**
     * Refreshes the GUI.
     */  
  	public void refresh() {
  		if (isClosing) return;
  		selectedFile = chooser.getSelectedFile();
  		// if one or fewer video engines available, don't show this at all!
  		if (buttonMap.size()<2) {
  			chooser.setAccessory(null);  			
        chooser.validate();
  			return;
  		}
      // count the video engine choices
  		int count = 0;
      boolean isButtonSelected = false;
      for (JRadioButton button: buttonMap.keySet()) {
      	if (button.isSelected())
      		isButtonSelected = true;
      	VideoType type = buttonMap.get(button);
      	for (FileFilter filter: type.getFileFilters()) {
      		if (selectedFile!=null && filter.accept(selectedFile)) {
      			count++;
      			continue;
      		}
      	}
      }
      if (count<2) {
  	    chooser.setAccessory(emptyPanel);
      }
      else {
  	    chooser.setAccessory(count<2? emptyPanel: videoEnginePanel);
        if (!isButtonSelected) {
      		boolean wantsQT = isQTPreferred();
          for (JRadioButton button: buttonMap.keySet()) {          	
          	boolean isQT = button.getActionCommand().indexOf("QT")>-1; //$NON-NLS-1$
          	button.setSelected((isQT&&wantsQT) || (!isQT&&!wantsQT));
          }
        }      	
      }
      chooser.validate();
      chooser.repaint();
  	}
  	
    /**
     * Responds to property change event.
     */  
  	public void propertyChange(PropertyChangeEvent e) {
      String name = e.getPropertyName();
      if (name.toLowerCase().indexOf("closing")>-1) { //$NON-NLS-1$
      	isClosing = true;
      }
      else if (chooser.getAccessory()==null) {
      	return;
      }
      else if (name.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
        refresh();
      }
  	}
  }

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
