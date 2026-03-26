import xml.etree.ElementTree as ET
import csv
import os

def get_local_name(node):
    """Helper to strip namespace prefixes from tags."""
    return node.tag.split('}')[-1] if '}' in node.tag else node.tag

def parse_robust_etecnic(xml_input_path, csv_output_path):
    expanded_path = os.path.expanduser(xml_input_path)
    
    if not os.path.exists(expanded_path):
        print(f"Error: File not found at {expanded_path}")
        return

    try:
        tree = ET.parse(expanded_path)
        root = tree.getroot()
        extracted_count = 0

        with open(csv_output_path, mode='w', newline='', encoding='utf-8') as csv_file:
            writer = csv.writer(csv_file)
            writer.writerow(['node_id', 'latitude', 'longitude', 'address', 'city'])

            # Use .iter() to find all elements regardless of depth or namespace
            for site in root.iter():
                if get_local_name(site) == 'energyInfrastructureSite':
                    
                    # 1. Check for Operator ID (ES*ETE)
                    is_etecnic = False
                    operator_id = ""
                    
                    # Search inside the current site for the operator tag
                    for elem in site.iter():
                        if get_local_name(elem) == 'operator':
                            operator_id = elem.get('id', '')
                            if "ETE" in operator_id:
                                is_etecnic = True
                                break
                    
                    if is_etecnic:
                        # 2. Extract Data using local name matching
                        site_id = site.get('id', 'unknown_id')
                        name = "Unknown"
                        lat = "0.0"
                        lon = "0.0"
                        street = "N/A"
                        city = "N/A"

                        for sub_elem in site.iter():
                            local_tag = get_local_name(sub_elem)
                            
                            if local_tag == 'value' and sub_elem.text:
                                # Prioritize names and addresses
                                text = sub_elem.text
                                if "Dirección:" in text: street = text.replace("Dirección: ", "").strip()
                                elif "Municipio:" in text: city = text.replace("Municipio: ", "").strip()
                                # Capture name if it's the first generic value we find and don't have one yet
                                elif name == "Unknown" and "Ajuntament" in text or "Constantí" in text:
                                    name = text

                            elif local_tag == 'latitude': lat = sub_elem.text
                            elif local_tag == 'longitude': lon = sub_elem.text

                        writer.writerow([site_id, name, lat, lon, street, city])
                        extracted_count += 1

        print(f"Success! Extracted {extracted_count} Etecnic stations to {csv_output_path}")

    except Exception as e:
        print(f"An error occurred: {e}")

# --- Execution ---
xml_path = '~/Desktop/Uni/Hackathon_Mobility/cargadores.xml'
parse_robust_etecnic(xml_path, 'etecnic_nodes.csv')