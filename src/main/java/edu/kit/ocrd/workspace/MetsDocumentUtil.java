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
import edu.kit.ocrd.dao.ModsIdentifier;
import edu.kit.ocrd.dao.PageFeatures;
import edu.kit.ocrd.workspace.entity.ClassificationMetadata;
import edu.kit.ocrd.workspace.entity.GenreMetadata;
import edu.kit.ocrd.workspace.entity.GroundTruthProperties;
import edu.kit.ocrd.workspace.entity.LanguageMetadata;
import edu.kit.ocrd.workspace.entity.MetsFile;
import edu.kit.ocrd.workspace.entity.MetsIdentifier;
import edu.kit.ocrd.workspace.entity.MetsProperties;
import edu.kit.ocrd.workspace.entity.PageMetadata;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.fzk.tools.xml.JaxenUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility handling METS document.
 */
public class MetsDocumentUtil extends MetsUtil {

  /**
   * Logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(MetsDocumentUtil.class);

  /**
   * Extract MetsFile instances from METS document.
   *
   * @param metsDocument METS document.
   * @param resourceId Resource ID of METS document.
   * @param version Version of METS document.
   *
   * @return List with all found files.
   */
  public static List<MetsFile> extractMetsFiles(Document metsDocument, String resourceId, Integer version) {
    LOGGER.info("Extract files from METS document. ResourceID: {}, Version: {}", resourceId, version);
    List<MetsFile> metsFiles = new ArrayList<>();
    List nodes = JaxenUtil.getNodes(metsDocument, metsMap.get(FILE_GROUPS), getNamespaces());
    LOGGER.trace("Found {} fileGrp(s)", nodes.size());
    for (Object node : nodes) {
      Element fileGrpElement = (Element) node;
      String use = JaxenUtil.getAttributeValue(fileGrpElement, "./@USE");
      List fileNodes = JaxenUtil.getNodes(fileGrpElement, "./mets:file", getNamespaces());
      LOGGER.trace("Found fileGrp with USE: {} containing {} file(s)", use, fileNodes.size());
      for (Object node2 : fileNodes) {
        Element fileElement = (Element) node2;
        String id = JaxenUtil.getAttributeValue(fileElement, "./@ID");
        String pageId;
        try {
          pageId = JaxenUtil.getAttributeValue(metsDocument, "//mets:div[./mets:fptr/@FILEID='" + id + "']/@ID", getNamespaces());
        } catch (ArrayIndexOutOfBoundsException aioobe) {
          // Try to find pageId using old style
          pageId = JaxenUtil.getAttributeValue(fileElement, "./@GROUPID");
        }
        String mimetype = JaxenUtil.getAttributeValue(fileElement, "./@MIMETYPE");
        String url = JaxenUtil.getAttributeValue(fileElement, "./mets:FLocat/@xlink:href", getNamespaces());
        LOGGER.trace("Found file with id: {}, pageId: {}, mimetype: {}, url: {}", id, pageId, mimetype, url);
        metsFiles.add(new MetsFile(resourceId, version, id, mimetype, pageId, use, url));
      }
    }
    return metsFiles;
  }

  /**
   * Extract all metadata from METS.
   *
   * @param metsDocument METS file.
   * @param resourceId Resource ID of METS document.
   * @return MetsMetadata holding all metadata.
   *
   * @throws Exception An error occurred during parsing METS file.
   */
  public static MetsProperties extractMetadataFromMets(final Document metsDocument, String resourceId) throws Exception {
    MetsProperties metsMetadata = new MetsProperties();
    metsMetadata.setResourceId(resourceId);
    // define XPaths
    Element root = metsDocument.getRootElement();
    String[] values = JaxenUtil.getValues(root, metsMap.get(TITLE), getNamespaces());
    if (values.length >= 1) {
      metsMetadata.setTitle(values[0]);
    }
    values = JaxenUtil.getValues(root, metsMap.get(SUB_TITLE), getNamespaces());
    if (values.length >= 1) {
      metsMetadata.setSubTitle(values[0]);
    }
    values = JaxenUtil.getValues(root, metsMap.get(YEAR), getNamespaces());
    if (values.length >= 1) {
      metsMetadata.setYear(values[0]);
    }
    values = JaxenUtil.getValues(root, metsMap.get(LICENSE), getNamespaces());
    if (values.length >= 1) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < values.length; i++) {
        if (values[i].trim().length() > 0) {
          if (builder.length() > 0) {
            builder.append(", ");
          }
          builder.append(values[i]);
        }
      }
      metsMetadata.setLicense(builder.toString());
    }
    values = JaxenUtil.getValues(root, metsMap.get(AUTHOR), getNamespaces());
    if (values.length >= 1) {
      metsMetadata.setAuthor(values[0]);
    }
    values = JaxenUtil.getValues(root, metsMap.get(NUMBER_OF_IMAGES), getNamespaces());
    metsMetadata.setNoOfPages(values.length);

    values = JaxenUtil.getValues(root, metsMap.get(PUBLISHER), getNamespaces());
    if (values.length >= 1) {
      metsMetadata.setPublisher(values[0]);
    }
    values = JaxenUtil.getValues(root, metsMap.get(PHYSICAL_DESCRIPTION), getNamespaces());
    if (values.length >= 1) {
      metsMetadata.setPhysicalDescription(values[0]);
    }
    values = JaxenUtil.getValues(root, metsMap.get(PPN), getNamespaces());
    if (values.length >= 1) {
      metsMetadata.setPpn(values[0]);
    }
    return metsMetadata;
  }

  /**
   * Extract all identifiers from METS.
   *
   * @param metsDocument METS file.
   * @param resourceId Resource ID of METS document.
   * @return List of MetsIdentifier holding all identifiers.
   *
   * @throws Exception An error occurred during parsing METS file.
   */
  public static List<MetsIdentifier> extractIdentifierFromMets(final Document metsDocument, final String resourceId) throws Exception {
    List<MetsIdentifier> metsIdentifierList = new ArrayList<>();
    Element root = metsDocument.getRootElement();
    List identifierList = JaxenUtil.getNodes(root, metsMap.get(UNIQUE_IDENTIFIER), getNamespaces());
    if (!identifierList.isEmpty()) {
      for (Object identifierObject : identifierList) {
        // Determine type and id 
        Element identifier = (Element) identifierObject;
        String type = getAttribute(identifier, "type");
        String id = identifier.getValue();
        metsIdentifierList.add(new MetsIdentifier(resourceId, type, id));
      }
    }
    return metsIdentifierList;
  }

  /**
   * Extract all language metadata from METS.
   *
   * @param metsDocument METS file.
   * @param resourceId Resource ID of METS document.
   * @return List of LanguageMetadata holding all language metadata.
   *
   * @throws Exception An error occurred during parsing METS file.
   */
  public static List<LanguageMetadata> extractLanguageMetadataFromMets(final Document metsDocument, final String resourceId) throws Exception {
    List<LanguageMetadata> languageList = new ArrayList<>();
    Element root = metsDocument.getRootElement();
    String[] values = JaxenUtil.getValues(root, metsMap.get(LANGUAGE), getNamespaces());
    if (values.length >= 1) {
      for (String language : values) {
        if (language.trim().length() > 1) {
          languageList.add(new LanguageMetadata(resourceId, language.trim()));
        }
      }
    }
    return languageList;
  }

  /**
   * Extract all classification metadata from METS.
   *
   * @param metsDocument METS file.
   * @param resourceId Resource ID of METS document.
   * @return List of ClassificationMetadata holding all classification metadata.
   *
   * @throws Exception An error occurred during parsing METS file.
   */
  public static List<ClassificationMetadata> extractClassificationMetadataFromMets(final Document metsDocument, final String resourceId) throws Exception {
    List<ClassificationMetadata> classificationList = new ArrayList<>();
    Element root = metsDocument.getRootElement();
    String[] values = JaxenUtil.getValues(root, metsMap.get(CLASSIFICATION), getNamespaces());
    if (values.length >= 1) {
      for (String classification : values) {
        if (classification.trim().length() > 1) {
          classificationList.add(new ClassificationMetadata(resourceId, classification.trim()));
        }
      }
    }
    return classificationList;
  }

  /**
   * Extract all genre metadata from METS.
   *
   * @param metsDocument METS file.
   * @param resourceId Resource ID of METS document.
   * @return List of ClassificationMetadata holding all genre metadata.
   *
   * @throws Exception An error occurred during parsing METS file.
   */
  public static List<GenreMetadata> extractGenreMetadataFromMets(final Document metsDocument, final String resourceId) throws Exception {
    List<GenreMetadata> genreList = new ArrayList<>();
    Element root = metsDocument.getRootElement();
    String[] values = JaxenUtil.getValues(root, metsMap.get(GENRE), getNamespaces());
    if (values.length >= 1) {
      for (String genre : values) {
        if (genre.trim().length() > 1) {
          genreList.add(new GenreMetadata(resourceId, genre.trim()));
        }
      }
    }
    return genreList;
  }

  /**
   * Extract all ground truth metadata from METS.
   *
   * @param metsDocument METS file.
   * @param resourceId Resource ID of METS document.
   * @return List of PageMetadata holding all ground truth metadata.
   *
   * @throws Exception An error occurred during parsing METS file.
   */
  public static List<PageMetadata> extractGroundTruthFeaturesFromMets(final Document metsDocument, final String resourceId) throws Exception {
    List<PageMetadata> pageMetadataList = new ArrayList<>();
    Element root = metsDocument.getRootElement();
    List physicalList = JaxenUtil.getNodes(root, metsMap.get(PHYSICAL_MAP), getNamespaces());
    if (!physicalList.isEmpty()) {
      Element structMap = (Element) physicalList.get(0);
      List pageList = JaxenUtil.getNodes(structMap, metsMap.get(PAGE_NODES), getNamespaces());
      if (!pageList.isEmpty()) {
        for (Object pageObject : pageList) {
          // Determine order, id and dmdid. 
          Element pageNode = (Element) pageObject;
          String order = getAttribute(pageNode, "ORDER");
          String id = getAttribute(pageNode, "ID");
          String dmdId = getAttribute(pageNode, "DMDID");
          String[] features = JaxenUtil.getValues(root, "//mets:dmdSec[@ID='" + dmdId + "']/mets:mdWrap[@OTHERMDTYPE='GT']/mets:xmlData/gt:gt/gt:state/@prop", getNamespaces());
          for (String feature : features) {
            pageMetadataList.add(new PageMetadata(resourceId, Integer.getInteger(order), id, GroundTruthProperties.get(feature)));
          }
        }
      }
    }
    return pageMetadataList;
  }

  /**
   * Extract all 'URLs' of referenced page.xml files.
   *
   * @param metsDocument METS file.
   * @return List of text regions.
   *
   * @throws Exception An error occurred during parsing METS file.
   */
  public static List<String> extractPageUrls(final Document metsDocument) throws Exception {
    List<String> pageUrls = new ArrayList<>();
    Element root = metsDocument.getRootElement();
    String[] values = JaxenUtil.getAttributesValues(root, "//mets:file[@MIMETYPE='application/vnd.prima.page+xml']/mets:FLocat/@xlink:href", getNamespaces());
      for (String href : values) {
        pageUrls.add(href);
      }
    return pageUrls;
  }

  /**
   * Convert entities to dao served to client.
   *
   * @param metadataEntity Metadata of METS document.
   * @param languages List with all languages of document.
   * @param classifications List of all classifications of document.
   * @param genres List of all genres of document.
   * @param pages List of all ground truth features of document.
   * @param identifiers List of all identifiers of document.
   * @return Metadata as a DAO
   */
  public static MetsMetadata convertEntityToDao(MetsProperties metadataEntity,
          List<LanguageMetadata> languages,
          List<ClassificationMetadata> classifications,
          List<GenreMetadata> genres,
          List<PageMetadata> pages,
          List<MetsIdentifier> identifiers) {
    MetsMetadata dao = new MetsMetadata();
    if (metadataEntity != null) {
      dao.setTitle(metadataEntity.getTitle());
      dao.setSubTitle(metadataEntity.getSubTitle());
      dao.setYear(metadataEntity.getYear());
      dao.setAuthor(metadataEntity.getAuthor());
      dao.setPublisher(metadataEntity.getPublisher());
      dao.setLicence(metadataEntity.getLicense());
      dao.setNoOfPages(metadataEntity.getNoOfPages());
      dao.setPhysicalDescription(metadataEntity.getPhysicalDescription());
    }
    if (languages != null) {
      List<String> languageList = new ArrayList<>();
      languages.forEach((language) -> {
        languageList.add(language.getLanguage());
      });
      dao.setLanguage(languageList);
    }
    if (classifications != null) {
      List<String> classificationList = new ArrayList<>();
      classifications.forEach((classification) -> {
        classificationList.add(classification.getClassification());
      });
      dao.setClassification(classificationList);
    }
    if (genres != null) {
      List<String> genreList = new ArrayList<>();
      genres.forEach((genre) -> {
        genreList.add(genre.getGenre());
      });
      dao.setGenre(genreList);
    }
    if (pages != null) {
      List<PageFeatures> pageList = new ArrayList<>();
      Map<String, PageFeatures> pageMap = new HashMap<>();
      pages.forEach((page) -> {
        PageFeatures item = pageMap.get(page.getPageId());
        if (item == null) {
          item = new PageFeatures();
          item.setPageId(page.getPageId());
          item.setOrder(page.getOrder());
          pageMap.put(page.getPageId(), item);
          item.setFeatures(new ArrayList<>());
        }
        item.getFeatures().add(page.getFeature().toString());
      });
      pageMap.values().forEach((item) -> {
        pageList.add(item);
      });
      dao.setPages(pageList);
    }
    if (identifiers != null) {
      List<ModsIdentifier> modsIdentifierList = new ArrayList<>();
      identifiers.forEach((identifier) -> {
        modsIdentifierList.add(new ModsIdentifier(identifier.getType(), identifier.getIdentifier()));
      });
      dao.setModsIdentifier(modsIdentifierList);
    }
    return dao;
  }

  /**
   * Get value of attribute of given element.
   *
   * @param element Element
   * @param attribute Label of attribute.
   * @return Value or unknown if attribute is not set.
   */
  private static String getAttribute(Element element, String attribute) {
    String attributeValue = "unknown";
    if (element.getAttribute(attribute) != null) {
      attributeValue = element.getAttribute(attribute).getValue();
    }
    return attributeValue;
  }
}
