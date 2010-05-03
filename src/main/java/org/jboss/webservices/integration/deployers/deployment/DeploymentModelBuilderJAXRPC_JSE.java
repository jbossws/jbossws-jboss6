package org.jboss.webservices.integration.deployers.deployment;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.webservices.integration.util.ASHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * Creates new JAXRPC JSE deployment.
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXRPC_JSE extends AbstractDeploymentModelBuilder
{

   /**
    * Constructor.
    */
   DeploymentModelBuilderJAXRPC_JSE()
   {
      super();
   }

   /**
    * Creates new JAXRPC JSE deployment and registers it with deployment unit.
    * 
    * @param dep webservice deployment
    * @param unit deployment unit
    */
   @Override
   protected void build(final Deployment dep, final DeploymentUnit unit)
   {
      final JBossWebMetaData webMetaData = this.getAndPropagateAttachment(JBossWebMetaData.class, unit, dep);
      final WebservicesMetaData wsMetaData = this.getAndPropagateAttachment(WebservicesMetaData.class, unit, dep);

      this.log.debug("Creating JAXRPC JSE endpoints meta data model");
      for (WebserviceDescriptionMetaData wsd : wsMetaData.getWebserviceDescriptions())
      {
         for (PortComponentMetaData pcmd : wsd.getPortComponents())
         {
            final String servletName = pcmd.getServletLink();
            this.log.debug("JSE name: " + servletName);
            final ServletMetaData servletMD = ASHelper.getServletForName(webMetaData, servletName);
            final String servletClass = ASHelper.getEndpointName(servletMD);
            this.log.debug("JSE class: " + servletClass);

            this.newEndpoint(servletClass, servletName, dep);
         }
      }
   }

}
