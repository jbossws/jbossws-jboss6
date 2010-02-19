package org.jboss.webservices.integration.deployers.deployment;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.webservices.integration.util.ASHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeclaration;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;

/**
 * Creates new JAXWS EJB3 deployment.
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXWS_EJB3 extends AbstractDeploymentModelBuilder
{

   /**
    * Constructor.
    */
   DeploymentModelBuilderJAXWS_EJB3()
   {
      super();
   }

   /**
    * Creates new JAXWS EJB3 deployment and registers it with deployment unit.
    * 
    * @param dep webservice deployment
    * @param unit deployment unit
    */
   @Override
   protected void build(final Deployment dep, final DeploymentUnit unit)
   {
      this.getAndPropagateAttachment(WebServiceDeployment.class, unit, dep);
      this.getAndPropagateAttachment(JBossMetaData.class, unit, dep);

      this.log.debug("Creating JAXWS EJB3 endpoints meta data model");
      for (final WebServiceDeclaration container : ASHelper.getJaxwsEjbs(unit))
      {
         final String ejbName = container.getComponentName();
         this.log.debug("EJB3 name: " + ejbName);
         final String ejbClass = container.getComponentClassName();
         this.log.debug("EJB3 class: " + ejbClass);

         final Endpoint ep = this.newEndpoint(ejbClass, ejbName, dep);
         ep.setProperty(ASHelper.CONTAINER_NAME, container.getContainerName());
      }
   }

}
