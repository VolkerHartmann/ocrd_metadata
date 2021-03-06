/*
 * Copyright 2018 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.ocrd.workspace;

import edu.kit.ocrd.dao.MetsMetadata;
import edu.kit.ocrd.workspace.MetsDocumentUtil;
import edu.kit.ocrd.workspace.entity.ClassificationMetadata;
import edu.kit.ocrd.workspace.entity.GenreMetadata;
import edu.kit.ocrd.workspace.entity.LanguageMetadata;
import edu.kit.ocrd.workspace.entity.MetsFile;
import edu.kit.ocrd.workspace.entity.MetsIdentifier;
import edu.kit.ocrd.workspace.entity.MetsProperties;
import edu.kit.ocrd.workspace.entity.PageMetadata;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.fzk.tools.xml.JaxenUtil;
import org.jdom.Document;
import org.jdom.Namespace;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * Test for MetsDocumentUtil.
 */
public class MetsDocumentUtilTest {

  public MetsDocumentUtilTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of extractMetsFiles method, of class MetsDocumentUtil.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testConstructor() {
    MetsDocumentUtil metsDocumentUtil = new MetsDocumentUtil();
    assertNotNull(metsDocumentUtil);
  }

  /**
   * Test of extractMetsFiles method, of class MetsDocumentUtil.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testExtractMetsFiles() throws Exception {
    System.out.println("extractMetsFiles");
    File file = new File("src/test/resources/mets/validMets.xml");
    assertTrue("File exists!", file.exists());
    String resourceId = "resourceId";
    Integer version = 3;
    Document metsDocument = JaxenUtil.getDocument(file);
    List<MetsFile> expResult = null;
    List<MetsFile> result = MetsDocumentUtil.extractMetsFiles(metsDocument, resourceId, version);
    System.out.println(result.size());
    assertEquals(34, result.size());
//    assertEquals(result);
    // TODO review the generated test code and remove the default call to fail.
  }

  /**
   * Test of extractProperties method, of class MetsDocumentUtil.
   */
  @Test
  public void testExtractProperties() throws Exception {
    System.out.println("extractProperties");
    File file = new File("src/test/resources/mets/validMets.xml");
    assertTrue("File exists!", file.exists());
    String resourceId = "resourceId";
    Document metsDocument = JaxenUtil.getDocument(file);
    MetsProperties metsProperties = MetsDocumentUtil.extractMetadataFromMets(metsDocument, resourceId);
    assertEquals(metsProperties.getTitle(), "Der Herold");
    assertEquals(metsProperties.getPpn(), "PPN767137728");
    assertEquals(metsProperties.getResourceId(), resourceId);
  }

  /**
   * Test of extractProperties method, of class MetsDocumentUtil.
   */
  @Test
  public void testGetNamespaces() throws Exception {
    System.out.println("getNamespaces");
    File file = new File("src/test/resources/mets/validMets.xml");
    assertTrue("File exists!", file.exists());
    String resourceId = "resourceId";
    Document metsDocument = JaxenUtil.getDocument(file);
    Namespace[] namespaces = MetsDocumentUtil.getNamespaces();
    assertEquals(5, namespaces.length);
    Map<String, Namespace> map = new HashMap<>();
    for (Namespace ns : namespaces) {
      map.put(ns.getPrefix(), ns);
    }
    assertEquals(map.get("mets").getURI(), "http://www.loc.gov/METS/");
    assertEquals(map.get("mods").getURI(), "http://www.loc.gov/mods/v3");
    assertEquals(map.get("xlink").getURI(), "http://www.w3.org/1999/xlink");
    assertEquals(map.get("gt").getURI(), "http://www.ocr-d.de/GT/");
    assertEquals(map.get("page2017").getURI(), "http://schema.primaresearch.org/PAGE/gts/pagecontent/2017-07-15");
  }

  /**
   * Test of extractProperties method, of class MetsDocumentUtil.
   */
  @Test
  public void testConvertMetadata2Dao() throws Exception {
   System.out.println("testConvertMetadata2Dao");
    File file = new File("src/test/resources/mets/complete_mets.xml");
    assertTrue("File exists!", file.exists());
    String resourceId = "resourceId";
    Integer version = 3;
    Document metsDocument = JaxenUtil.getDocument(file);
    MetsProperties metsProperties = MetsDocumentUtil.extractMetadataFromMets(metsDocument, resourceId);
    assertEquals("Grundriss der Psychologie", metsProperties.getTitle());
    assertEquals("No PPN available!", metsProperties.getPpn());
    assertEquals(metsProperties.getResourceId(), resourceId);
    List<MetsFile> metsFiles = MetsDocumentUtil.extractMetsFiles(metsDocument, resourceId, version);
    System.out.println(metsFiles.size());
    assertEquals(16, metsFiles.size());
    List<LanguageMetadata> languages = MetsDocumentUtil.extractLanguageMetadataFromMets(metsDocument, resourceId);
    List<String> language = new ArrayList<>();
    for (LanguageMetadata lm : languages) {
      language.add(lm.getLanguage());
    }
    List<ClassificationMetadata> classifications = MetsDocumentUtil.extractClassificationMetadataFromMets(metsDocument, resourceId);
    List<String> classification = new ArrayList<>();
    for (ClassificationMetadata cm : classifications) {
      classification.add(cm.getClassification());
    }
    List<GenreMetadata> genres = MetsDocumentUtil.extractGenreMetadataFromMets(metsDocument, resourceId);
    List<String> genre = new ArrayList<>();
    for (GenreMetadata gm : genres) {
      genre.add(gm.getGenre());
    }
    List<PageMetadata> pages = MetsDocumentUtil.extractGroundTruthFeaturesFromMets(metsDocument, resourceId);
    List<MetsIdentifier> identifiers = MetsDocumentUtil.extractIdentifierFromMets(metsDocument, resourceId);
    MetsMetadata metsMetadata = MetsDocumentUtil.convertEntityToDao(metsProperties, languages, classifications, genres, pages, identifiers);
    assertEquals("Wilhelm Wundt", metsMetadata.getAuthor());
    assertEquals(classification, metsMetadata.getClassification());
    assertEquals(genre, metsMetadata.getGenre());
    assertEquals(language, metsMetadata.getLanguage());
    assertTrue(metsMetadata.getLicence().contains("OCR-D"));
    assertEquals(2, metsMetadata.getModsIdentifier().size());
    assertEquals(4, metsMetadata.getNoOfPages());
    assertEquals("XVI, 392 S.", metsMetadata.getPhysicalDescription());
    assertEquals("Engelmann", metsMetadata.getPublisher());
    assertEquals("", metsMetadata.getSubTitle());
    assertEquals("Grundriss der Psychologie", metsMetadata.getTitle());
    assertEquals("1896", metsMetadata.getYear());
  }
}
