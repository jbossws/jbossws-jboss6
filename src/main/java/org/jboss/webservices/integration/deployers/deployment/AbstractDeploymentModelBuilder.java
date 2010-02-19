package org.jboss.webservices.integration.deployers.deployment;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.metadata.serviceref.VirtualFileAdaptor;
import org.jboss.virtual.VirtualFile;
import org.jboss.webservices.integration.util.ASHelper;
import org.jboss.wsf.common.ResourceLoaderAdapter;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;
import org.jboss.wsf.spi.deployment.DeploymentModelFactory;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.deployment.WSFDeploymentException;

/**
 * Base class for all deployment model builders.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class AbstractDeploymentModelBuilder implements DeploymentModelBuilder
{

   /** WSDL, XSD and XML files filter. */
   private static final WSVirtualFileFilter WS_FILE_FILTER = new WSVirtualFileFilter();

   /** Logger. */
   protected final Logger log = Logger.getLogger(this.getClass());

   /** Deployment model factory. */
   private final DeploymentModelFactory deploymentModelFactory;

   /**
    * Constructor.
    */
   protected AbstractDeploymentModelBuilder()
   {
      super();

      // deployment factory
      final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
      this.deploymentModelFactory = spiProvider.getSPI(DeploymentModelFactory.class);
   }

   /**
    * @see org.jboss.webservices.integration.deployers.deployment.DeploymentModelBuilder#newDeploymentModel(DeploymentUnit)
    *
    * @param unit deployment unit
    */
   public final void newDeploymentModel(final DeploymentUnit unit)
   {
      final ArchiveDeployment dep = this.newDeployment(unit);

      this.build(dep, unit);

      dep.addAttachment(DeploymentUnit.class, unit);
      unit.addAttachment(Deployment.class, dep);
   }

   /**
    * Template method for subclasses to implement.
    *
    * @param dep webservice deployment
    * @param unit deployment unit
    */
   protected abstract void build(Deployment dep, DeploymentUnit unit);

   /**
    * Creates new Web Service endpoint.
    *
    * @param endpointClass endpoint class name
    * @param endpointName endpoint name
    * @param dep deployment
    * @return WS endpoint
    */
   protected final Endpoint newEndpoint(final String endpointClass, final String endpointName, final Deployment dep)
   {
      if (endpointName == null)
      {
         throw new NullPointerException("Null endpoint name");
      }

      if (endpointClass == null)
      {
         throw new NullPointerException("Null endpoint class");
      }

      final Endpoint endpoint = this.deploymentModelFactory.newEndpoint(endpointClass);
      endpoint.setShortName(endpointName);
      dep.getService().addEndpoint(endpoint);

      return endpoint;
   }

   /**
    * Creates new Web Service deployment.
    *
    * @param unit deployment unit
    * @return archive deployment
    */
   private ArchiveDeployment newDeployment(final DeploymentUnit unit)
   {
      this.log.debug("Creating new WS deployment model for: " + unit);
      final ArchiveDeployment dep = this.newDeployment(unit.getSimpleName(), unit.getClassLoader());

      if (unit instanceof VFSDeploymentUnit)
      {
         final VFSDeploymentUnit vfsUnit = (VFSDeploymentUnit) unit;
         final List<VirtualFile> virtualFiles = vfsUnit.getMetaDataFiles(AbstractDeploymentModelBuilder.WS_FILE_FILTER);
         final Set<UnifiedVirtualFile> uVirtualFiles = new HashSet<UnifiedVirtualFile>();
         for (VirtualFile vf : virtualFiles)
         {
            // Adding the roots of the virtual files.
            try
            {
               uVirtualFiles.add(new VirtualFileAdaptor(vf.getVFS().getRoot()));
            }
            catch (IOException ioe)
            {
               throw new WSFDeploymentException(ioe);
            }
         }
         dep.setMetadataFiles(new LinkedList<UnifiedVirtualFile>(uVirtualFiles));
      }

      if (unit.getParent() != null)
      {
         final String parentDeploymentName = unit.getParent().getSimpleName();
         final ClassLoader parentClassLoader = unit.getParent().getClassLoader();

         this.log.debug("Creating new WS deployment model for parent: " + unit.getParent());
         final ArchiveDeployment parentDep = this.newDeployment(parentDeploymentName, parentClassLoader);
         dep.setParent(parentDep);
      }

      if (unit instanceof VFSDeploymentUnit)
      {
         dep.setRootFile(new VirtualFileAdaptor(((VFSDeploymentUnit) unit).getRoot()));
      }
      else
      {
         dep.setRootFile(new ResourceLoaderAdapter(unit.getClassLoader()));
      }
      dep.setRuntimeClassLoader(unit.getClassLoader());
      final DeploymentType deploymentType = ASHelper.getRequiredAttachment(unit, DeploymentType.class);
      dep.setType(deploymentType);

      return dep;
   }

   /**
    * Creates new archive deployment.
    *
    * @param name deployment name
    * @param loader deployment loader
    * @return new archive deployment
    */
   private ArchiveDeployment newDeployment(final String name, final ClassLoader loader)
   {
      return (ArchiveDeployment) this.deploymentModelFactory.newDeployment(name, loader);
   }

   /**
    * Gets specified attachment from deployment unit. 
    * Checks it's not null and then propagates it to <b>dep</b>
    * attachments. Finally it returns attachment value.
    *
    * @param <A> class type
    * @param attachment attachment
    * @param unit deployment unit
    * @param dep deployment
    * @return attachment value if found in unit
    */
   protected final <A> A getAndPropagateAttachment(final Class<A> attachment, final DeploymentUnit unit,
         final Deployment dep)
   {
      final A attachmentValue = ASHelper.getOptionalAttachment(unit, attachment);

      if (attachmentValue != null)
      {
         dep.addAttachment(attachment, attachmentValue);
         return attachmentValue;
      }

      throw new IllegalStateException("Deployment unit does not contain " + attachment);
   }

}
