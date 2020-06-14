package com.metasys.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Update a file in a DAR
 */
@Mojo(name = "dar-update", defaultPhase = LifecyclePhase.PACKAGE)
public class DARUpdateMojo extends AbstractMojo {
	/**
	 * The path to the DAR file.
	 */
	@Parameter(property = "dar", required = true, alias = "dar")
	private String darPath;

	/**
	 * The name of the artifact to update.
	 */
	@Parameter(property = "mappings", alias = "mappings", required = true)
	private List<Mapping> mappings;

	/**
	 * The name of the artifact to update.
	 */
	@Parameter(property = "artifactName", alias = "artifactName", required = false)
	private String artifactName;

	/**
	 * The path to a new content
	 */
	@Parameter(property = "contentPath", alias = "contentPath", required = false)
	private String contentPath;

	/**
	 * The path where to save the final DAR.
	 */
	@Parameter(property = "targetPath", alias = "targetPath", required = true)
	private String targetPath;

	/**
	 * If the file can be overwritten or not
	 */
	@Parameter(property = "overwrite", alias = "overwrite", defaultValue = "false", required = true)
	private boolean overwrite;

	/**
	 * List all artifacts.
	 */
	@Parameter(property = "verbose", alias = "verbose", defaultValue = "false", required = true)
	private boolean verbose;

	@Override
	public void execute() {
		ZipFile zipFile;
		try {
			File tmpFile = new File(darPath);
			for (Mapping m : mappings) {
				m.setArtifactPath(getArtifactFilePath(m.getArtifactName()));

				zipFile = new ZipFile(tmpFile.getAbsoluteFile());
				File _tmpFile = processSingleArtifact(zipFile, m);
				if (_tmpFile != null) {
					if (!tmpFile.getName().equals(new File(darPath).getName())) {
						tmpFile.delete();
					}
					tmpFile = _tmpFile;
				}
				zipFile.close();
			}

			System.out.println("Moving updated DAR to " + targetPath);
			File targetFile = new File(targetPath);
			if (overwrite && targetFile.exists()) {
				targetFile.delete();
			}
			FileUtils.moveFile(tmpFile, targetFile);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getArtifactFilePath(String artifactName) {
		ZipFile zipFile;
		try {
			zipFile = new ZipFile(darPath);
			for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
				ZipEntry entryIn = (ZipEntry) e.nextElement();
				if (verbose) {
					System.out.println(entryIn.getName() + " " + entryIn.getSize());
				}
				if (entryIn.getName().contains(artifactName)) {
					InputStream is = zipFile.getInputStream(entryIn);
					DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = builderFactory.newDocumentBuilder();
					Document xmlDocument = builder.parse(is);
					XPath xPath = XPathFactory.newInstance().newXPath();
					String expression = "//contentStore/contentEntries/value";
					NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

					if (nodeList.getLength() > 0) {
						Node value = nodeList.item(0);
						return value.getAttributes().getNamedItem("filePath").getTextContent();
					}
				}
			}
		} catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	private File processSingleArtifact(ZipFile zipFile, Mapping mapping) throws IOException {
		System.out.println("Artifact's '" + mapping.getArtifactName() + "' content is located at: " + mapping.getArtifactPath());

		File tmpFile = File.createTempFile("dar-" + new File(darPath).getName(), ".zip");
		final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpFile));
		for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
			ZipEntry entryIn = (ZipEntry) e.nextElement();
			if (!entryIn.getName().contains(mapping.getArtifactPath())) {
				zos.putNextEntry(entryIn);
				InputStream is = zipFile.getInputStream(entryIn);
				byte[] buf = new byte[1024];
				int len;
				while ((len = is.read(buf)) > 0) {
					zos.write(buf, 0, len);
				}
			} else {
				File contentFile = new File(mapping.getContentPath());
				System.out.println("Updating " + entryIn.getName() + "modified on " + entryIn.getLastModifiedTime().toInstant().atZone(
						ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)) +
						" with " + contentFile.getAbsolutePath() + " size (" + contentFile.length() + ")");
				zos.putNextEntry(new ZipEntry(entryIn.getName()));
				zos.write(IOUtils.readFully(new FileInputStream(contentFile), (int) contentFile.length()));
			}
			zos.closeEntry();
		}
		zos.close();
		return tmpFile;
	}
}