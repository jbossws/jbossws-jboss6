package org.jboss.webservices.integration.deployers.deployment;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * Creates new JAXRPC EJB21 deployment.
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXRPC_EJB21 extends AbstractDeploymentModelBuilder
{

   /**
    * Constructor.
    */
   DeploymentModelBuilderJAXRPC_EJB21()
   {
      super();
   }

   /**
    * Creates new JAXRPC EJB21 deployment and registers it with deployment unit.
    * 
    * @param dep webservice deployment
    * @param unit deployment unit
    */
   @Override
   protected void build(final Deployment dep, final DeploymentUnit unit)
   {
      final JBossMetaData jbmd = this.getAndPropagateAttachment(JBossMetaData.class, unit, dep);
      final WebservicesMetaData wsMetaData = this.getAndPropagateAttachment(WebservicesMetaData.class, unit, dep);
      this.getAndPropagateAttachment(WebServiceDeployment.class, unit, dep);

      this.log.debug("Creating JAXRPC EJB21 endpoints meta data model");
      for (final WebserviceDescriptionMetaData webserviceDescriptionMD : wsMetaData.getWebserviceDescriptions())
      {
         for (final PortComponentMetaData portComponentMD : webserviceDescriptionMD.getPortComponents())
         {
            final String ejbName = portComponentMD.getEjbLink();
            this.log.debug("EJB21 name: " + ejbName);
            final JBossEnterpriseBeanMetaData beanMetaData = jbmd.getEnterpriseBean(ejbName);
            final String ejbClass = beanMetaData.getEjbClass();
            this.log.debug("EJB21 class: " + ejbClass);

            this.newEndpoint(ejbClass, ejbName, dep);
         }
      }
   }

}
