import ControlDashboard from "@/src/components/control-dashboard";
import { getOperationsSnapshot } from "@/src/features/operations/backend";

export default async function ControlPage() {
  const snapshot = await getOperationsSnapshot();

  return <ControlDashboard orders={snapshot.workOrders} />;
}

