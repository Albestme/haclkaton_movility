import OperationsDashboard from "@/src/components/operations-dashboard";
import { getOperationsSnapshot } from "@/src/features/operations/backend";

export default async function PanelPage() {
  const snapshot = await getOperationsSnapshot();

  return (
    <OperationsDashboard
      initialTechniciansData={snapshot.technicians}
      initialOrdersData={snapshot.workOrders}
    />
  );
}

