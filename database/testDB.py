import socket
import os
from dotenv import load_dotenv

load_dotenv()

host = 'database-1.cveau0o428yi.us-east-1.rds.amazonaws.com'
port = 5432

print(f"🔗 Testing connection to {host}...")

try:
    # 1. Test DNS again
    ip = socket.gethostbyname(host)
    print(f"✅ DNS Resolved: {ip}")

    # 2. Test Port 5432 (The "Correct" way)
    # Usamos socket.AF_INET (del módulo) y socket.SOCK_STREAM
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(5)
        result = s.connect_ex((host, port))
        if result == 0:
            print(f"✅ PORT {port} IS OPEN! You can connect now.")
        else:
            print(f"❌ PORT {port} IS CLOSED. Check AWS Security Groups/Inbound Rules.")

except Exception as e:
    print(f"❌ Connection failed: {e}")