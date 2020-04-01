package de.winkhaus.opcSimulator.opc;

import de.winkhaus.opcSimulator.jpa.MachineRepository;
import de.winkhaus.opcSimulator.model.Machine;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.ServerNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegateChain;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

public class SpsMuensterNamespace extends ManagedNamespace {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SubscriptionModel subscriptionModel;

    static final String NAMESPACE_URI = "urn:muenster:sps";
    static final String NODEFOLDER = "data";
    static final String COUNTER = "pieces";
    static final String MESSAGE = "messageId";
    static final String ACTIVE = "active";
    private static final String MACHINEID = "machine";

    private MachineRepository repository;
    private String machineId;
    ;

    public SpsMuensterNamespace(OpcUaServer server, MachineRepository repository, String machineId) {
        super(server, NAMESPACE_URI);
        this.machineId = machineId;
        this.repository = repository;
        subscriptionModel = new SubscriptionModel(server, this);
    }

    @Override
    protected void onStartup() {
        super.onStartup();

        NodeId folderNodeId = newNodeId(NODEFOLDER);

        UaFolderNode folderNode = new UaFolderNode(
                getNodeContext(),
                folderNodeId,
                newQualifiedName(NODEFOLDER),
                LocalizedText.english(NODEFOLDER)
        );

        getNodeManager().addNode(folderNode);

        // Make sure our new folder shows up under the server's Objects folder.
        folderNode.addReference(new Reference(
                folderNode.getNodeId(),
                Identifiers.Organizes,
                Identifiers.ObjectsFolder.expanded(),
                false
        ));

        // Add the rest of the nodes
        addVariableNodes(folderNode);

        // Set the EventNotifier bit on Server Node for Events.
        UaNode serverNode = getServer()
                .getAddressSpaceManager()
                .getManagedNode(Identifiers.Server)
                .orElse(null);

        if (serverNode instanceof ServerNode) {
            ((ServerNode) serverNode).setEventNotifier(ubyte(1));

            // Post a bogus Event every couple seconds
            getServer().getScheduledExecutorService().scheduleAtFixedRate(() -> {
                try {
                    BaseEventNode eventNode = getServer().getEventFactory().createEvent(
                            newNodeId(UUID.randomUUID()),
                            Identifiers.BaseEventType
                    );

                    getServer().getEventBus().post(eventNode);

                    eventNode.delete();
                } catch (Throwable e) {
                    logger.error("Error creating EventNode: {}", e.getMessage(), e);
                }
            }, 0, 2, TimeUnit.SECONDS);
        }
    }

    private void addVariableNodes(UaFolderNode rootNode) {
        addDynamicNodes(rootNode);
    }

    private void addDynamicNodes(UaFolderNode rootNode) {

        rootNode.addOrganizes(node(ACTIVE, Identifiers.Boolean, AttributeDelegateChain.create(
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        return new DataValue(new Variant(repository.findByMachineId(machineId).getStatus().isActive()));
                    }
                },
                ValueLoggingDelegate::new
        )));

        rootNode.addOrganizes(node(COUNTER, Identifiers.Double,  AttributeDelegateChain.create(
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        Machine machine = repository.findByMachineId(machineId);

                        if(machine.getStatus().isActive()){
                            Double oldValue = machine.getCounter().getPieces();
                            Double newValue = oldValue + Double.valueOf(1);
                            machine.getCounter().setPieces(newValue);
                            repository.save(machine);
                        }
                        return new DataValue(new Variant(machine.getCounter().getPieces()));
                    }
                },
                ValueLoggingDelegate::new
        )));

        rootNode.addOrganizes(node(MESSAGE, Identifiers.Integer,  AttributeDelegateChain.create(
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        return new DataValue(new Variant(repository.findByMachineId(machineId).getMessage().getNumber()));
                    }
                },
                ValueLoggingDelegate::new
        )));
        rootNode.addOrganizes(node(MACHINEID, Identifiers.String,  AttributeDelegateChain.create(
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        return new DataValue(new Variant(machineId));
                    }
                },
                ValueLoggingDelegate::new
        )));
    }

    private UaVariableNode node(String name, NodeId typeId, AttributeDelegate delegate){
        Variant variant = new Variant(0);

        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId(String.format("%s/%s",NODEFOLDER, name)))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        node.setValue(new DataValue(variant));
        node.setAttributeDelegate(delegate);
        getNodeManager().addNode(node);

        return node;
    }


    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }
}
