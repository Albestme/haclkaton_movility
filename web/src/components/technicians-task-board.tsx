"use client";

import { useMemo, useState } from "react";
import { Technician, TechnicianStatus, WorkOrder, WorkOrderPriority } from "@/src/features/operations/types";

type TechniciansTaskBoardProps = {
  technicians: Technician[];
  initialOrders: WorkOrder[];
};

const technicianStatusLabel: Record<TechnicianStatus, string> = {
  available: "Disponible",
  on_route: "En ruta",
  working: "Trabajando",
  offline: "Fuera de linea",
};

const priorityLabel: Record<WorkOrderPriority, string> = {
  correctivo_critico: "Correctivo critico",
  correctivo_no_critico: "Correctivo no critico",
  mantenimiento_preventivo_programado: "Mantenimiento preventivo",
  puesta_en_marcha: "Puesta en marcha",
  visita_diagnostico: "Visita de diagnostico",
  high: "Alta",
  medium: "Media",
  low: "Baja",
};

const priorityBadge: Record<WorkOrderPriority, string> = {
  correctivo_critico: "bg-red-100 text-red-700",
  correctivo_no_critico: "bg-orange-100 text-orange-700",
  mantenimiento_preventivo_programado: "bg-amber-100 text-amber-700",
  puesta_en_marcha: "bg-blue-100 text-blue-700",
  visita_diagnostico: "bg-emerald-100 text-emerald-700",
  high: "bg-red-100 text-red-700",
  medium: "bg-amber-100 text-amber-700",
  low: "bg-emerald-100 text-emerald-700",
};

export default function TechniciansTaskBoard({ technicians, initialOrders }: TechniciansTaskBoardProps) {
  const [orders, setOrders] = useState<WorkOrder[]>(initialOrders);
  const [draggingOrderId, setDraggingOrderId] = useState<string | null>(null);
  const [dropTarget, setDropTarget] = useState<string | null>(null);

  const ordersByTechnician = useMemo(() => {
    return technicians.reduce<Record<string, WorkOrder[]>>((acc, technician) => {
      acc[technician.id] = orders.filter((order) => order.technicianId === technician.id);
      return acc;
    }, {});
  }, [orders, technicians]);

  const unassignedOrders = useMemo(
    () => orders.filter((order) => !order.technicianId),
    [orders],
  );

  function handleDragStart(orderId: string, event: React.DragEvent<HTMLDivElement>) {
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", orderId);
    setDraggingOrderId(orderId);
  }

  function handleDrop(technicianId: string | undefined, event: React.DragEvent<HTMLElement>) {
    event.preventDefault();
    const orderId = event.dataTransfer.getData("text/plain") || draggingOrderId;

    setDropTarget(null);
    setDraggingOrderId(null);

    if (!orderId) {
      return;
    }

    setOrders((current) =>
      current.map((order) => {
        if (order.id !== orderId) {
          return order;
        }

        if (technicianId) {
          return {
            ...order,
            technicianId,
            status: order.status === "pending" ? "assigned" : order.status,
          };
        }

        return {
          ...order,
          technicianId: undefined,
          status: order.status === "assigned" ? "pending" : order.status,
        };
      }),
    );
  }

  function handleDragEnd() {
    setDraggingOrderId(null);
    setDropTarget(null);
  }

  return (
    <div className="space-y-6">
      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {technicians.map((technician) => {
          const assignedOrders = ordersByTechnician[technician.id] ?? [];
          const isDropActive = dropTarget === technician.id;

          return (
            <article
              key={technician.id}
              className={`rounded-2xl border bg-white p-5 shadow-sm transition ${
                isDropActive ? "border-sky-400 ring-2 ring-sky-100" : "border-slate-200"
              }`}
              onDragOver={(event) => {
                event.preventDefault();
                setDropTarget(technician.id);
              }}
              onDragLeave={() => {
                if (dropTarget === technician.id) {
                  setDropTarget(null);
                }
              }}
              onDrop={(event) => handleDrop(technician.id, event)}
            >
              <h2 className="text-lg font-semibold">{technician.name}</h2>
              <p className="mt-1 text-sm text-slate-500">Zona: {technician.zone}</p>
              <p className="mt-1 text-sm text-slate-500">Estado: {technicianStatusLabel[technician.status]}</p>

              <div className="mt-4 space-y-3">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Tareas asignadas ({assignedOrders.length})
                </p>

                {assignedOrders.length > 0 ? (
                  assignedOrders.map((order) => (
                    <TaskCard
                      key={order.id}
                      order={order}
                      onDragStart={handleDragStart}
                      onDragEnd={handleDragEnd}
                    />
                  ))
                ) : (
                  <div className="rounded-lg border border-dashed border-slate-300 px-3 py-4 text-center text-sm text-slate-500">
                    Suelta aqui una tarea para asignarla.
                  </div>
                )}
              </div>
            </article>
          );
        })}
      </section>

      <section
        className={`rounded-2xl border bg-white p-5 shadow-sm transition ${
          dropTarget === "unassigned" ? "border-sky-400 ring-2 ring-sky-100" : "border-slate-200"
        }`}
        onDragOver={(event) => {
          event.preventDefault();
          setDropTarget("unassigned");
        }}
        onDragLeave={() => {
          if (dropTarget === "unassigned") {
            setDropTarget(null);
          }
        }}
        onDrop={(event) => handleDrop(undefined, event)}
      >
        <div className="mb-3 flex items-center justify-between gap-2">
          <h3 className="text-lg font-semibold">Tareas sin asignar</h3>
          <span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-700">
            {unassignedOrders.length}
          </span>
        </div>

        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {unassignedOrders.length > 0 ? (
            unassignedOrders.map((order) => (
              <TaskCard
                key={order.id}
                order={order}
                onDragStart={handleDragStart}
                onDragEnd={handleDragEnd}
              />
            ))
          ) : (
            <div className="rounded-lg border border-dashed border-slate-300 px-3 py-4 text-sm text-slate-500">
              No hay tareas pendientes de asignacion.
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

type TaskCardProps = {
  order: WorkOrder;
  onDragStart: (orderId: string, event: React.DragEvent<HTMLDivElement>) => void;
  onDragEnd: () => void;
};

function TaskCard({ order, onDragStart, onDragEnd }: TaskCardProps) {
  return (
    <div
      draggable
      onDragStart={(event) => onDragStart(order.id, event)}
      onDragEnd={onDragEnd}
      className="cursor-grab rounded-xl border border-slate-200 bg-slate-50 p-3 active:cursor-grabbing"
    >
      <div className="flex items-start justify-between gap-2">
        <p className="text-sm font-semibold text-slate-800">{order.id}</p>
        <span className={`rounded-full px-2 py-1 text-[11px] font-semibold ${priorityBadge[order.priority]}`}>
          {priorityLabel[order.priority]}
        </span>
      </div>
      <p className="mt-1 text-sm text-slate-700">{order.siteName}</p>
      <p className="mt-1 text-xs text-slate-500">{order.city} - {order.scheduledAt}</p>
    </div>
  );
}

