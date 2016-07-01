package com.rsicms.rsuite.utils.mo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.VersionHistory;
import com.reallysi.rsuite.api.VersionSpecifier;
import com.reallysi.rsuite.api.VersionType;
import com.reallysi.rsuite.api.browse.BrowseInfo;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.NonXmlObjectSource;
import com.reallysi.rsuite.api.control.ObjectCheckInOptions;
import com.reallysi.rsuite.api.control.ObjectInsertOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.control.ObjectUpdateOptions;
import com.reallysi.rsuite.api.control.XmlObjectSource;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.xml.XPathEvaluator;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.rsicms.rsuite.utils.xml.DomUtils;
import com.rsicms.rsuite.utils.xml.TransformUtils;

/**
 * A collection of static MO utility methods.
 */
public class MOUtils {

  /**
   * Get the input stream of an MO, exposing options RSuite's ManagedObjectService does not.
   * 
   * @param context
   * @param mo
   * @param includeXMLDeclaration
   * @param includeDoctypeDeclaration
   * @param encoding
   * @return The MO's input stream, after applying options RSuite's API doesn't offer.
   * @throws RSuiteException
   * @throws UnsupportedEncodingException
   * @throws TransformerException
   */
  public static InputStream getInputStream(ExecutionContext context, ManagedObject mo,
      boolean includeXMLDeclaration, boolean includeDoctypeDeclaration, String encoding)
      throws RSuiteException, UnsupportedEncodingException, TransformerException {
    Element elem = mo.getElement();
    String str = DomUtils.serializeToString(context, elem, includeXMLDeclaration,
        includeDoctypeDeclaration, encoding);
    return new ByteArrayInputStream(str.getBytes(encoding));
  }

  /**
   * Get a display name for the MO. Fails over to local name when display name is null.
   * 
   * @param mo
   * @return a display name for the MO.
   * @throws RSuiteException
   */
  public static String getDisplayName(ManagedObject mo) throws RSuiteException {
    return StringUtils.isBlank(mo.getDisplayName()) ? mo.getLocalName() : mo.getDisplayName();
  }

  /**
   * Get the qualified element name of an MO.
   * 
   * @param mo
   * @return the qualified element name.
   * @throws RSuiteException Thrown if unable to determine the given MO's qualified element name.
   */
  public static String getQualifiedElementName(ManagedObject mo) throws RSuiteException {
    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotBlank(mo.getNamespaceURI())) {
      sb.append(mo.getNamespaceURI()).append(":");
    }
    return sb.append(mo.getLocalName()).toString();
  }

  /**
   * Get a display name for the MO, without throwing an exception.
   * 
   * @param mo
   * @return a display name for the MO, or an empty string if an exception is encountered.
   */
  public static String getDisplayNameQuietly(ManagedObject mo) {
    try {
      return getDisplayName(mo);
    } catch (Exception e) {
      return StringUtils.EMPTY;
    }
  }

  /**
   * Get a managed object from a CA item.
   * 
   * @param context
   * @param user
   * @param caItem
   * @return Container or null. Null return when the CA item is not a content assembly, content
   *         assembly reference, or CANode.
   * @throws RSuiteException
   */
  public static ManagedObject getManagedObject(ExecutionContext context, User user,
      ContentAssemblyItem caItem) throws RSuiteException {
    ManagedObject mo = null;
    if (caItem instanceof ManagedObject) {
      mo = (ManagedObject) caItem;
    } else if (caItem instanceof ManagedObjectReference) {
      mo = context.getManagedObjectService().getManagedObject(user,
          ((ManagedObjectReference) caItem).getTargetId());
    }
    return mo;
  }

  /**
   * Get an <code>ObjectSource</code> from a <code>File</code>.
   * 
   * @param context
   * @param filename
   * @param content
   * @param encoding Only used if the file is believed to be XML.
   * @return Either an instance of <code>XmlObjectSource</code> or <code>NonXmlObjectSource</code>.
   * @throws IOException Thrown if unable to get bytes from given file.
   */
  public static ObjectSource getObjectSource(ExecutionContext context, String filename,
      File content, String encoding) throws IOException {
    FileInputStream fis = new FileInputStream(content);
    try {
      return getObjectSource(context, filename, fis, encoding);
    } finally {
      IOUtils.closeQuietly(fis);
    }
  }

  /**
   * Get an <code>ObjectSource</code> from an <code>InputStream</code>.
   * 
   * @param context
   * @param filename
   * @param content
   * @param encoding Only used if the file is believed to be XML.
   * @return Either an instance of <code>XmlObjectSource</code> or <code>NonXmlObjectSource</code>.
   * @throws IOException Thrown if unable to get bytes from input stream.
   */
  public static ObjectSource getObjectSource(ExecutionContext context, String filename,
      InputStream content, String encoding) throws IOException {
    return getObjectSource(context, filename, IOUtils.toByteArray(content), encoding);
  }

  /**
   * Get an <code>ObjectSource</code> from a <code>String</code>.
   * 
   * @param context
   * @param filename
   * @param content
   * @param encoding Only used if the file is believed to be XML.
   * @return Either an instance of <code>XmlObjectSource</code> or <code>NonXmlObjectSource</code>.
   * @throws IOException Thrown if unable to get bytes from given file.
   */
  public static ObjectSource getObjectSource(ExecutionContext context, String filename,
      String content, String encoding) throws IOException {
    return getObjectSource(context, filename,
        IOUtils.toByteArray(new StringReader(content), encoding), encoding);
  }

  /**
   * Get an <code>ObjectSource</code> from a <code>byte</code> array.
   * 
   * @param context
   * @param filename
   * @param content
   * @param encoding Only used if the file is believed to be XML.
   * @return Either an instance of <code>XmlObjectSource</code> or <code>NonXmlObjectSource</code>.
   * @throws IOException Thrown if unable to get bytes from given file.
   */
  public static ObjectSource getObjectSource(ExecutionContext context, String filename,
      byte[] content, String encoding) throws IOException {
    if (context.getRSuiteServerConfiguration()
        .isTreatAsXmlFileExtension(FilenameUtils.getExtension(filename))) {
      return new XmlObjectSource(content, encoding);
    } else {
      return new NonXmlObjectSource(content);
    }
  }

  /**
   * Get the insert options for the given object source and name.
   * 
   * @param context
   * @param objectSource
   * @param objectName
   * @param advisor A local MO advisor to use. May be null.
   * @return The insert options for either a new XML MO or non-XML MO.
   */
  public static ObjectInsertOptions getObjectInsertOptions(ExecutionContext context,
      ObjectSource objectSource, String objectName, ManagedObjectAdvisor advisor) {

    // In 4.1.12 an additional constructor was added to ObjectInsertOptions
    // There are constructors for Alias[] and String[] so a null no longer
    // works. Using String[] because the 4.1.9 constructor used String[].
    String[] aliases = null;

    ObjectInsertOptions insertOptions = new ObjectInsertOptions(objectName, aliases, // aliases:
                                                                                     // handled
                                                                                     // by
                                                                                     // MO
                                                                                     // advisor;
        null, // collections
        true, // force the document to be loaded?
        true); // validate?

    insertOptions.setAdvisor(advisor);

    insertOptions.setContentType(context.getConfigurationService().getMimeMappingCatalog()
        .getMimeTypeByExtension(FilenameUtils.getExtension(objectName)));

    if (objectSource instanceof NonXmlObjectSource) {
      insertOptions.setExternalFileName(objectName);
      insertOptions.setFileName(objectName);
      insertOptions.setDisplayName(objectName);
    }

    return insertOptions;

  }

  /**
   * Get the update options for the given object source and name.
   * 
   * @param objectSource
   * @param objectName
   * @param advisor
   * @return The update options for either an existing XML MO or non-XML MO.
   */
  public static ObjectUpdateOptions getObjectUpdateOptions(ObjectSource objectSource,
      String objectName, ManagedObjectAdvisor advisor) {
    ObjectUpdateOptions options = new ObjectUpdateOptions();
    options.setExternalFileName(objectName);
    options.setDisplayName(objectName);
    options.setValidate(true);
    options.setAdvisor(advisor);
    return options;
  }

  /**
   * Determine if an MO, and optionally, its sub-MOs, are checked out.
   * 
   * @param moService
   * @param user
   * @param id
   * @param includeSubMos Submit true to check the MO's sub-MOs.
   * @return True if the MO is checked out. When checkSubMos is true, may also return true when a
   *         sub MO is checked out.
   * @throws RSuiteException
   */
  public static boolean isCheckedOut(ManagedObjectService moService, User user, String id,
      boolean includeSubMos) throws RSuiteException {
    if (moService.isCheckedOut(user, id)) {
      return true;
    }
    if (includeSubMos) {
      ManagedObject mo = moService.getManagedObject(user, id);
      if (mo.hasChildren()) {
        for (ManagedObject subMo : mo.listDescendantManagedObjects()) {
          if (subMo.isCheckedout())
            return true;
        }
      }
    }
    return false;
  }

  /**
   * Check out the MO, if able to. If already checked out by another user, an exception is thrown.
   * If already checked out to the specified user, no action is performed.
   * 
   * @param context
   * @param user
   * @param id
   * @return true if this method checked the MO out; false if the MO was already checked out to the
   *         specified user.
   * @throws RSuiteException
   */
  public static boolean checkout(ExecutionContext context, User user, String id)
      throws RSuiteException {
    ManagedObjectService moService = context.getManagedObjectService();
    if (!moService.isCheckedOut(user, id)) {
      moService.checkOut(user, id);
      return true;
    } else {
      if (moService.isCheckedOutButNotByUser(user, id)) {
        throw new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR,
            MOUtilsMessageProperties.get("error.mo.checked.out.by.different.user",
                moService.getCheckOutInfo(id).getUserId(), id));
      }
      return false;
    }
  }

  /**
   * Get the version specifier for the version of the MO that is immediately before the current
   * version.
   * 
   * @param context
   * @param user
   * @param id
   * @return Version specifier for the identified MO that immediately precedes the current version,
   *         or null if there is only one version of the MO.
   * @throws RSuiteException
   */
  public static VersionSpecifier getPreviousVersionSpecifier(ExecutionContext context, User user,
      String id) throws RSuiteException {

    VersionHistory vh = context.getManagedObjectService().getVersionHistory(user, id);

    if (vh.size() >= 2) {
      /*
       * Use the second entry in the list of versions, as the first (index=0) is the current
       * version.
       */
      return new VersionSpecifier(id, vh.getVersionEntries().get(1).getRevisionNumber());
    }

    // The current version is the latest version.
    return null;

  }

  /**
   * Set metadata entries for the given moid.
   * 
   * @param user
   * @param moService
   * @param moid
   * @param metaDataItems
   * @throws RSuiteException
   */
  public static void setMetadataEntries(User user, ManagedObjectService moService, String moid,
      List<MetaDataItem> metaDataItems) throws RSuiteException {
    moService.setMetaDataEntries(user, moid, metaDataItems);
  }

  /**
   * Delete metadata from an MO by metadata name. If the metadata repeats, all metadata items with
   * the specified metadata name will be deleted.
   * <p>
   * IMPROVE: This method could be optimized for repeating LMD by calling
   * ManagedObjectService#processMetaDataChangeSet().
   * 
   * @param user
   * @param moService
   * @param moid
   * @param lmdName
   * @throws RSuiteException
   */
  public static void deleteMetadataEntries(User user, ManagedObjectService moService, String moid,
      String lmdName) throws RSuiteException {
    if (StringUtils.isNotBlank(moid) && StringUtils.isNotBlank(lmdName)) {
      ManagedObject mo = moService.getManagedObject(user, moid);
      if (mo != null) {
        for (MetaDataItem mdItem : mo.getMetaDataItems()) {
          if (mdItem.getName().equals(lmdName)) {
            moService.removeMetaDataEntry(user, moid, mdItem);
          }
        }
      }
    }
  }

  /**
   * Load a RSuite <code>ManagedObject</code>.
   * 
   * @param context
   * @param user
   * @param filename
   * @param is
   * @param encoding
   * @param moAdvisor
   * @return The <code>ManagedObject</code> loaded in RSuite.
   * @throws IOException
   * @throws RSuiteException
   */
  public static ManagedObject load(ExecutionContext context, User user, String filename,
      InputStream is, String encoding, ManagedObjectAdvisor moAdvisor)
      throws IOException, RSuiteException {
    return load(context, user, filename, getObjectSource(context, filename, is, encoding),
        moAdvisor);
  }

  /**
   * Create a new managed object in RSuite.
   * 
   * @param context
   * @param user
   * @param filename
   * @param objectSource
   * @param moAdvisor
   * @return The <code>ManagedObject</code> loaded in RSuite.
   * @throws RSuiteException
   */
  public static ManagedObject load(ExecutionContext context, User user, String filename,
      ObjectSource objectSource, ManagedObjectAdvisor moAdvisor) throws RSuiteException {
    return context.getManagedObjectService().load(user, objectSource,
        getObjectInsertOptions(context, objectSource, filename, moAdvisor));
  }

  /**
   * Find out if an MO has the specified QName.
   * 
   * @param mo
   * @param qname
   * @return True if the MO is an XML MO with the specified QName.
   * @throws RSuiteException
   */
  public static boolean hasMatchingQName(ManagedObject mo, QName qname) throws RSuiteException {
    return (mo != null && !mo.isNonXml() && mo.getLocalName().equals(qname.getLocalPart()) && (
    // If both are blank, they're both in the default namespace
    (StringUtils.isBlank(mo.getNamespaceURI()) && StringUtils.isBlank(qname.getNamespaceURI()))
        || mo.getNamespaceURI().equals(qname.getNamespaceURI())));
  }

  /**
   * Apply a transform to an MO and update the same MO with the tranform's result.
   * <p>
   * If you don't already have a session, use the signature that accepts <code>User</code> instead
   * of <code>Session</code>.
   * 
   * @param context
   * @param session A valid session that identifies the user to operate as. The session's key is
   *        also passed in as an XSL parameter when includeStandardRSuiteXslParams is true.
   * @param mo The managed object to check out (when not already checked out), apply the transform
   *        to, update with the transform result, and check back in.
   * @param xslUri URI of the XSL to apply.
   * @param xslParams Optional parameters to pass into the XSL. Null may be sent in. Hint: List
   *        <String> parameters are received as a sequence, at least with Saxon.
   * @param includeStandardRSuiteXslParams Submit true to ensure XSLT parameters that RSuite
   *        typically provides are included herein, specifically including the base RSuite URL and a
   *        session key.
   * @param baseRSuiteUrl Only used when includeStandardRSuiteXslParams is true.
   * @param resultEncoding
   * @param versionNote The new MO version's note.
   * @throws RSuiteException
   * @throws URISyntaxException
   * @throws TransformerException
   * @throws SAXException
   * @throws IOException
   */
  public static void applyTransformAndUpdate(ExecutionContext context, Session session,
      ManagedObject mo, URI xslUri, Map<String, Object> xslParams,
      boolean includeStandardRSuiteXslParams, String baseRSuiteUrl, String resultEncoding,
      String versionNote)
      throws RSuiteException, URISyntaxException, TransformerException, SAXException, IOException {
    User user = session.getUser();
    ManagedObjectService moService = context.getManagedObjectService();
    boolean createdCheckOut = false;
    InputStream transformResult = null;
    try {
      // Make sure the user has the check out.
      createdCheckOut = checkout(context, user, mo.getId());

      // Perform transform
      transformResult = TransformUtils.transform(context, session, mo,
          context.getXmlApiManager().getTransformer(xslUri), xslParams,
          includeStandardRSuiteXslParams, baseRSuiteUrl);

      // Update the MO
      ObjectSource objectSource =
          getObjectSource(context, "file.xml", transformResult, resultEncoding);
      moService.update(user, mo.getId(), objectSource,
          getObjectUpdateOptions(objectSource, StringUtils.EMPTY, null));

      // Check in the MO
      ObjectCheckInOptions checkInOptions = new ObjectCheckInOptions();
      checkInOptions.setVersionType(VersionType.MINOR);
      checkInOptions.setVersionNote(versionNote);
      moService.checkIn(user, mo.getId(), checkInOptions);
    } finally {
      // If this method checked the MO out and it is still checked out,
      // cancel it.
      if (createdCheckOut && moService.isCheckedOutAuthor(user, mo.getId())) {
        moService.undoCheckout(user, mo.getId());
      }

      IOUtils.closeQuietly(transformResult);
    }
  }

  /**
   * Find out if the provided MO is a sub-MO.
   * 
   * @param moService
   * @param user
   * @param mo
   * @return True if a sub-MO; else, false. False returned for containers and top-level MOs,
   *         including non-XML MOs. Not sure about references.
   * @throws RSuiteException
   */
  public static boolean isSubMo(ManagedObjectService moService, User user, ManagedObject mo)
      throws RSuiteException {
    return !mo.getId().equals(moService.getRootManagedObjectId(user, mo.getId()));
  }

  /**
   * Find out if the MO is not a sub-MO.
   * 
   * @param moService
   * @param user
   * @param mo
   * @return True if not a sub-MO; else false.
   * @throws RSuiteException
   */
  public static boolean isNotSubMo(ManagedObjectService moService, User user, ManagedObject mo)
      throws RSuiteException {
    return !isSubMo(moService, user, mo);
  }

  /**
   * Throw an exception if the MO is a sub-MO.
   * 
   * @param moService
   * @param user
   * @param mo
   * @throws RSuiteException
   */
  public static void throwIfSubMo(ManagedObjectService moService, User user, ManagedObject mo)
      throws RSuiteException {
    if (isSubMo(moService, user, mo)) {
      throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
          new StringBuilder("'").append(getDisplayNameQuietly(mo)).append("' (ID: ")
              .append(mo.getId()).append(") is a sub-MO.").toString());
    }
  }

  /**
   * Throw an exception if the MO is not a sub-MO.
   * 
   * @param moService
   * @param user
   * @param mo
   * @throws RSuiteException
   */
  public static void throwIfNotSubMo(ManagedObjectService moService, User user, ManagedObject mo)
      throws RSuiteException {
    if (isNotSubMo(moService, user, mo)) {
      throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
          new StringBuilder("'").append(getDisplayNameQuietly(mo)).append("' (ID: ")
              .append(mo.getId()).append(") is not a sub-MO.").toString());
    }
  }

  /**
   * Get one of the given sub-MO's siblings --either the preceding or following based on value of of
   * the "preceding" parameter.
   * 
   * @param moService
   * @param user
   * @param mo Sub-MO to get a sibling of.
   * @param preceding Submit true for the MO's preceding sub-MO or false for its following sub-MO.
   * @return A sibling sub-MO or, when one doesn't exist, null.
   * @throws RSuiteException Thrown if the given MO is not a sub-MO.
   */
  public static ManagedObject getSiblingSubMo(ManagedObjectService moService, User user,
      ManagedObject mo, boolean preceding) throws RSuiteException {
    throwIfNotSubMo(moService, user, mo);
    int increment = 20;
    int start = 0;
    int end = start + increment;
    BrowseInfo browseInfo = moService.getChildManagedObjects(user,
        moService.getRootManagedObjectId(user, mo.getId()), start, end);
    List<ManagedObject> moList;
    ManagedObject previousMo = null;
    boolean returnNext = false;
    while (browseInfo.getTotal() > 0) {
      moList = browseInfo.getManagedObjects();
      for (ManagedObject candidateMo : moList) {
        if (returnNext) {
          return candidateMo;
        }
        if (candidateMo.getId().equals(mo.getId())) {
          if (preceding) {
            return previousMo;
          } else {
            returnNext = true;
          }
        }
        previousMo = candidateMo;
      }

      // Get another batch.
      start += increment;
      end += increment;
      browseInfo = moService.getChildManagedObjects(user,
          moService.getRootManagedObjectId(user, mo.getId()), start, end);
    }
    return null;
  }

  /**
   * Get the sub-MO immediately preceding the provided one.
   * 
   * @param moService
   * @param user
   * @param mo
   * @return The sub-MO preceding the given one, or null when there isn't one.
   * @throws RSuiteException Thrown when not given a sub-MO.
   */
  public static ManagedObject getPrecedingSubMo(ManagedObjectService moService, User user,
      ManagedObject mo) throws RSuiteException {
    return getSiblingSubMo(moService, user, mo, true);
  }

  /**
   * Get the sub-MO immediately following the provided one.
   * 
   * @param moService
   * @param user
   * @param mo
   * @return The sub-MO following the given one, or null when there isn't one.
   * @throws RSuiteException Thrown when not given a sub-MO.
   */
  public static ManagedObject getFollowingSubMo(ManagedObjectService moService, User user,
      ManagedObject mo) throws RSuiteException {
    return getSiblingSubMo(moService, user, mo, false);
  }

  /**
   * Add elements into an ancestor MO before or after the specified location.
   * 
   * @param moService
   * @param user
   * @param ancestorMoId The ID of the MO to add the new nodes within.
   * @param adjacentNodeXPath An XPath expression identifying an existing descendant node within the
   *        ancestor that the new nodes are to be added before or after. An exception will be thrown
   *        if this doesn't identify a node within the ancestor.
   * @param insertBefore Submit true to add new nodes before the node identified by
   *        adjacentNodeXPath; else, submit false to inserted after it.
   * @param eval The XPath evaluator to use within the ancestor MO.
   * @param newNodes The new nodes to add within the ancestor.
   * @throws RSuiteException
   */
  public static void addNodesIntoExistingMo(ManagedObjectService moService, User user,
      String ancestorMoId, String adjacentNodeXPath, boolean insertBefore, XPathEvaluator eval,
      List<Node> newNodes) throws RSuiteException {
    if (newNodes != null && newNodes.size() > 0) {
      // Require the ancestor MO be checked out by the requesting user.
      if (!isCheckedOut(moService, user, ancestorMoId, false)) {
        throw new RSuiteException(RSuiteException.ERROR_OBJECT_NOT_CHECKED_OUT,
            "Check out the MO before attempting to insert content within.");
      }

      // Obtain the node to insert before or within.
      ManagedObject ancestorMo = moService.getManagedObject(user, ancestorMoId);
      Element ancestorElem = ancestorMo.getElement();
      Node adjacentNode = eval.executeXPathToNode(adjacentNodeXPath, ancestorElem);
      if (adjacentNode == null) {
        throw new RSuiteException(RSuiteException.ERROR_OBJECT_NOT_FOUND,
            "'" + adjacentNodeXPath + "' does not identify a node within '"
                + getDisplayNameQuietly(ancestorMo) + "' (ID: " + ancestorMo.getId() + ").");
      }
      if (!insertBefore) {
        if (adjacentNode.getNextSibling() != null) {
          adjacentNode = adjacentNode.getNextSibling();
        } else {
          adjacentNode = null;
        }
      }

      // Insert the new nodes.
      Node parentNode = adjacentNode.getParentNode();
      Document doc = parentNode.getOwnerDocument();
      for (Node node : newNodes) {
        doc.adoptNode(node);
        if (adjacentNode == null) {
          parentNode.appendChild(node);
        } else {
          parentNode.insertBefore(node, adjacentNode);
        }
      }

      // Update the ancestor in RSuite
      ObjectSource objectSource = new XmlObjectSource(ancestorElem);
      moService.update(user, ancestorMoId, objectSource,
          getObjectUpdateOptions(objectSource, StringUtils.EMPTY, null));
    }
  }
}
