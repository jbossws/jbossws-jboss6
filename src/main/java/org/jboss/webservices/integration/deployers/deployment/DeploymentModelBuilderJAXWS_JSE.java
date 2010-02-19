package org.jboss.webservices.integration.deployers.deployment;

import java.util.List;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.webservices.integration.util.ASHelper;
import org.jboss.wsf.spi.deployment.Deployment;

/**
 * Creates new JAXWS JSE deployment.
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXWS_JSE extends AbstractDeploymentModelBuilder
{

   /**
    * Constructor.
    */
   DeploymentModelBuilderJAXWS_JSE()
   {
      super();
   }

   /**
    * Creates new JAXWS JSE deployment and registers it with deployment unit.
    * 
    * @param dep webservice deployment
    * @param unit deployment unit
    */
   @Override
   protected void build(final Deployment dep, final DeploymentUnit unit)
   {
      this.getAndPropagateAttachment(JBossWebMetaData.class, unit, dep);

      this.log.debug("Creating JAXWS JSE endpoints meta data model");
      final List<ServletMetaData> servlets = ASHelper.getJaxwsServlets(unit);
      for (ServletMetaData servlet : servlets)
      {
         final String servletName = servlet.getName();
         this.log.debug("JSE name: " + servletName);
         final String servletClass = ASHelper.getEndpointName(servlet);
         this.log.debug("JSE class: " + servletClass);

         this.newEndpoint(servletClass, servletName, dep);
      }
   }

}
