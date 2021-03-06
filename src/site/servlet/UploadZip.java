package site.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import net.minidev.json.JSONObject;
import site.DocubricksSite;
import site.DocumentDirectory;
import site.record.RecordDocument;
import site.util.EvFileUtil;

/**
 * Servlet implementation class Test
 */
@WebServlet("/UploadZip")
@MultipartConfig
public class UploadZip extends DocubricksServlet
	{
	private static final long serialVersionUID = 1L;

	

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
		{
		RecordDocument rec=null;
		try(DocubricksSite session=new DocubricksSite())
			{
			JSONObject retob=new JSONObject();
			session.fromSession(request.getSession());
			
			Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">
//		    String fileName = filePart.getSubmittedFileName();
			if(filePart!=null)
				{
      	rec=new RecordDocument();
				rec.documentOwnerID=session.session.userID;
				System.out.println("owner id "+rec.documentOwnerID);
      	rec.allocate(session);

				DocumentDirectory docdir=rec.getDir();
				//TODO delete entry if zip fails
				
		    InputStream fis = filePart.getInputStream();
				
				final int BUFFER = 2048;
	      BufferedOutputStream dest = null;
	      CheckedInputStream checksum = new CheckedInputStream(fis, new Adler32());
	      ZipInputStream zis = new ZipInputStream(new BufferedInputStream(checksum));
	      ZipEntry entry;
	      while((entry = zis.getNextEntry()) != null) 
	      	{
	      	System.out.println("Extracting: " +entry);
	      	int count;
	      	byte data[] = new byte[BUFFER];
	      	// write the files to the disk
	      	//System.out.println("name: "+entry.getName());
	      	
      		File f=new File(docdir.getRoot(),entry.getName());
      		//System.out.println(f);
	      	if(entry.isDirectory())
	      		{
	      		f.mkdirs();
	      		}
	      	else
	      		{
	      		f.getParentFile().mkdirs();
		      	FileOutputStream fos = new FileOutputStream(f);  // kopek problem 2
		      	dest = new BufferedOutputStream(fos, BUFFER);
		      	while ((count = zis.read(data, 0, BUFFER)) != -1) 
		      		dest.write(data, 0, count);
		      	dest.flush();
		      	dest.close();
	      		}
	      	}
	      zis.close();
	      System.out.println("Checksum: "+checksum.getChecksum().getValue());
	      
	      //scary idea: putting links into zip?
	      //scary idea: putting .. dirs into zip?
	      //hmm. what if the zip contains everything in one subdir? kind of logical. then everything should be shifted down

	      docdir.removeTopDir();
	      File fileXML=docdir.getDocubricksXML();
	      if(fileXML!=null)
	      	{
	      	
	      	
	      	//TODO should allocate a record FIRST.
	      	rec.documentXML=EvFileUtil.readFile(fileXML);
	      	fileXML.delete();
	      	
	      	System.err.println("wheee!");
	      	System.err.println("the doc "+rec.documentXML);
	      	session.daoDocument.update(rec);
	  			retob.put("id", ""+rec.id);
					response.getWriter().append(retob.toJSONString());
	      	}
	      else
	      	{
	      	rec.delete(session);
	      	retob.put("id","-1");
	      	retob.put("error", "Zip does not contain a docubrick");
	      	response.getWriter().append(retob.toJSONString());
//						response.sendError(404, "Zip does not contain a docubrick");
	      	}
				
				}
			else
				{
				response.sendError(404, "Zip does not contain a docubrick");
				}
			}
		catch (Exception e)
			{
			//Delete the document if it was created but upload failed
			if(rec!=null)
				{
				try(DocubricksSite session=new DocubricksSite())
					{
					rec.delete(session);
					}
				catch (Exception e2)
					{
					e2.printStackTrace();
					throw new ServletException(e.getMessage());
					}
				}
				
			
			e.printStackTrace();
			throw new ServletException(e.getMessage());
			}
		}


	}
