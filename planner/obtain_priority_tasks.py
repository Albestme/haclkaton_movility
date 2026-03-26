import csv
import random
import os
from collections import Counter

def generate_and_verify_priorities(input_csv, output_csv):
    """
    Generates realistic operational priorities (0-4) and prints a 
    distribution summary immediately [cite: 37-43].
    """
    if not os.path.exists(input_csv):
        print(f"Error: {input_csv} not found.")
        return

    # Statistical setup: Mu=0 and Sigma=1.4 ensures 0 and 1 are most frequent
    mu = 0.0
    sigma = 1.4
    
    # Priority labels based on Etecnic's typology [cite: 37-42]
    labels = {
        0: "Commissioning (Posada en marxa)",
        1: "Preventive (Manteniment preventiu)",
        2: "Diagnosis (Visita de diagnosi)",
        3: "Non-critical Corrective (Correctiu no crític)",
        4: "Critical Corrective (Correctiu crític)"
    }

    updated_rows = []
    priority_counts = Counter()

    try:
        with open(input_csv, mode='r', encoding='utf-8') as file:
            reader = csv.DictReader(file)
            headers = reader.fieldnames + ['priority', 'work_type']
            
            for row in reader:
                # Distribution logic: f(x) = |N(0, 1.4)|
                val = abs(random.gauss(mu, sigma))
                priority = int(round(val))
                if priority > 4: priority = 4
                
                # Update row data
                row['priority'] = priority
                row['work_type'] = labels[priority].split(" (")[0] # Clean label
                
                updated_rows.append(row)
                priority_counts[priority] += 1

        # Save to CSV
        with open(output_csv, mode='w', newline='', encoding='utf-8') as file:
            writer = csv.DictWriter(file, fieldnames=headers, extrasaction='ignore')
            writer.writeheader()
            writer.writerows(updated_rows)

        # --- Integrated Print Summary ---
        total = len(updated_rows)
        print("\n" + "="*70)
        print(f"DATA DISTRIBUTION SUMMARY (N={total})")
        print("="*70)
        print(f"{'ID':<4} | {'WORK TYPE':<35} | {'COUNT':<8} | {'PERCENTAGE'}")
        print("-"*70)
        
        for p_id in range(5):
            count = priority_counts[p_id]
            percentage = (count / total) * 100 if total > 0 else 0
            bar = "█" * int(percentage // 2)
            print(f"{p_id:<4} | {labels[p_id]:<35} | {count:<8} | {percentage:>6.1f}% {bar}")
        
        print("="*70)
        print(f"File successfully saved to: {output_csv}\n")

    except Exception as e:
        print(f"Critical Error: {e}")

generate_and_verify_priorities('data/etecnic_nodes.csv', 'data/etecnic_nodes_priority.csv')