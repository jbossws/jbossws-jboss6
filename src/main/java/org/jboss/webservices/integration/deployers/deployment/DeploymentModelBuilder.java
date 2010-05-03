package org.jboss.webservices.integration.deployers.deployment;

import org.jboss.deployers.structure.spi.DeploymentUnit;

/**
 * Deployment builder interface.
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
interface DeploymentModelBuilder
{

   /**
    * Creates Web Service deployment model and associates it with deployment.
    * 
    * @param unit deployment unit
    */
   void newDeploymentModel(DeploymentUnit unit);

}
