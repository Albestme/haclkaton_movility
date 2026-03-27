"use client";

import { useMemo, useState } from "react";
import { Technician, TechnicianStatus, WorkOrder } from "@/src/features/operations/types";

type TechniciansTaskBoardProps = {
  technicians: Technician[];
  initialOrders: WorkOrder[];
};

const technicianStatusLabel: Record<TechnicianStatus, string> = {
  available: "Disponible",
  on_route: "En ruta",
  working: "Trabajando",
  offline: "Fuera de línea",
};

const priorityBadge: Record<string, string> = {
  "Reparaci\u00F3 cr\u00EDtica": "bg-red-100 text-red-700",
  "Visita de diagnosi": "bg-emerald-100 text-emerald-700",
  "Manteniment preventiu": "bg-amber-100 text-amber-700",
};

export default function TechniciansTaskBoard({ technicians, initialOrders }: TechniciansTaskBoardProps) {
  const [orders, setOrders] = useState<WorkOrder[]>(initialOrders);
  const [draggingOrderId, setDraggingOrderId] = useState<string | null>(null);
  const [dropTarget, setDropTarget] = useState<string | null>(null);
  const [savedMessage, setSavedMessage] = useState<string | null>(null);

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
    const orderId = Number(event.dataTransfer.getData("text/plain") || draggingOrderId);

    setDropTarget(null);
    setDraggingOrderId(null);

    if (!Number.isFinite(orderId)) {
      return;
    }

    setOrders((current) =>
      current.map((order) => {
        if (order.incidence_id !== orderId) {
          return order;
        }

        if (technicianId) {
          return {
            ...order,
            technicianId,
            status: order.status === "Nova" ? "Assignada" : order.status,
          };
        }

        return {
          ...order,
          technicianId: undefined,
          status: order.status === "Assignada" ? "Nova" : order.status,
        };
      }),
    );
  }

  function handleDragEnd() {
    setDraggingOrderId(null);
    setDropTarget(null);
  }

  function handleSaveSelection() {
    try {
      localStorage.setItem("technicians_task_board_orders", JSON.stringify(orders));
      setSavedMessage("Selección guardada correctamente");
      setTimeout(() => setSavedMessage(null), 3000);
    } catch {
      setSavedMessage("Error al guardar la selección");
      setTimeout(() => setSavedMessage(null), 3000);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold">Asignación de tareas</h2>
          <p className="text-sm text-slate-600">Arrastra tareas para reasignarlas entre técnicos</p>
        </div>
        <div className="flex flex-col items-end gap-2">
          <button
            onClick={handleSaveSelection}
            className="flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700 transition"
          >
            <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            Guardar selección
          </button>
          {savedMessage && (
            <div className={`text-xs font-medium rounded px-3 py-1 ${
              savedMessage.includes("Error") 
                ? "bg-red-100 text-red-700" 
                : "bg-emerald-100 text-emerald-700"
            }`}>
              {savedMessage}
            </div>
          )}
        </div>
      </div>

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
                      key={order.incidence_id}
                      order={order}
                      onDragStart={handleDragStart}
                      onDragEnd={handleDragEnd}
                    />
                  ))
                ) : (
                  <div className="rounded-lg border border-dashed border-slate-300 px-3 py-4 text-center text-sm text-slate-500">
                    Suelta aquí una tarea para asignarla.
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
                key={order.incidence_id}
                order={order}
                onDragStart={handleDragStart}
                onDragEnd={handleDragEnd}
              />
            ))
          ) : (
            <div className="rounded-lg border border-dashed border-slate-300 px-3 py-4 text-sm text-slate-500">
              No hay tareas pendientes de asignación.
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
      onDragStart={(event) => onDragStart(String(order.incidence_id), event)}
      onDragEnd={onDragEnd}
      className="cursor-grab rounded-xl border border-slate-200 bg-slate-50 p-3 active:cursor-grabbing"
    >
      <div className="flex items-start justify-between gap-2">
        <p className="text-sm font-semibold text-slate-800">INC-{order.incidence_id}</p>
        <span className={`rounded-full px-2 py-1 text-[11px] font-semibold ${priorityBadge[order.priority] ?? "bg-slate-100 text-slate-700"}`}>
          {order.priority}
        </span>
      </div>
      <p className="mt-1 text-sm text-slate-700">Cargador {order.charger_id}</p>
      <p className="mt-1 text-xs text-slate-500">Est.: {order.estimated_duration_min} min</p>
    </div>
  );
}

