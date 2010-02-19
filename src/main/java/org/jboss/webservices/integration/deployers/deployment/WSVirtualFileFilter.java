package org.jboss.webservices.integration.deployers.deployment;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileFilterWithAttributes;
import org.jboss.virtual.VisitorAttributes;

/**
 * WS file filter for files with the '.wsdl', or '.xsd' or '.xml' suffix. 
 * 
 * @author <a href="mailto:dbevenius@jboss.com">Daniel Bevenius</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSVirtualFileFilter implements VirtualFileFilterWithAttributes
{

   /** The tree walking attributes. */
   private VisitorAttributes attributes;

   /**
    * Constructor. 
    */
   WSVirtualFileFilter()
   {
      this(VisitorAttributes.RECURSE_LEAVES_ONLY);
   }

   /**
    * Constructor.
    * 
    * @param attributes visit attributes
    */
   WSVirtualFileFilter(final VisitorAttributes attributes)
   {
      this.attributes = attributes;
   }

   /**
    * Gets VisitorAttributes for this instance.
    * 
    * @return visitor attributes
    */
   public VisitorAttributes getAttributes()
   {
      return this.attributes;
   }

   /**
    * Accepts files that end with '.wsdl' or '.xsd' or '.xml'.
    *
    * @param file to analyze
    * @return true if expected file extension, false otherwise
    */
   public boolean accepts(final VirtualFile file)
   {
      if (file == null)
      {
         return false;
      }

      final String fileName = file.getName().toLowerCase();
      final boolean hasWsdlSuffix = fileName.endsWith(".wsdl");
      final boolean hasXsdSuffix = fileName.endsWith(".xsd");
      final boolean hasXmlSuffix = fileName.endsWith(".xml");

      return hasWsdlSuffix || hasXsdSuffix || hasXmlSuffix;
   }

}
