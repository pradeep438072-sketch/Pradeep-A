import os
import json
import datetime
import uuid
from flask import Flask, request, jsonify, make_response

app = Flask(__name__)
app.secret_key = os.environ.get("SECRET_KEY", "medivoice-ai-secret-key-2026")

# In-memory storage with default seed data for demonstration
demo_user = {
    "id": "usr_default_001",
    "name": "Sarah Jenkins",
    "email": "sarah.jenkins@example.com",
    "phone": "+1 (555) 234-5678",
    "password": "password123"
}

users = [demo_user]

default_medicines = [
    {
        "id": "med_001",
        "user_id": "usr_default_001",
        "name": "Paracetamol",
        "type": "Tablet",
        "dosage_name": "500 mg tablet",
        "dosage_amount": 500,
        "dosage_unit": "mg",
        "daily_dosage": "1 tablet",
        "dosage_limit": "2 tablets per day",
        "times_per_day": 2,
        "reminder_times": ["08:00 AM", "08:00 PM"],
        "start_date": "2026-09-01",
        "end_date": "2026-10-01",
        "food_relation": "After Food",
        "notes": "Take with full glass of water for fever or pain."
    },
    {
        "id": "med_002",
        "user_id": "usr_default_001",
        "name": "Amoxicillin",
        "type": "Capsule",
        "dosage_name": "250 mg capsule",
        "dosage_amount": 250,
        "dosage_unit": "mg",
        "daily_dosage": "1 capsule",
        "dosage_limit": "3 capsules per day",
        "times_per_day": 3,
        "reminder_times": ["08:00 AM", "02:00 PM", "09:00 PM"],
        "start_date": "2026-09-20",
        "end_date": "2026-09-30",
        "food_relation": "With Food",
        "notes": "Complete full antibiotic course."
    },
    {
        "id": "med_003",
        "user_id": "usr_default_001",
        "name": "Atorvastatin",
        "type": "Tablet",
        "dosage_name": "20 mg tablet",
        "dosage_amount": 20,
        "dosage_unit": "mg",
        "daily_dosage": "1 tablet",
        "dosage_limit": "1 tablet per day",
        "times_per_day": 1,
        "reminder_times": ["09:00 PM"],
        "start_date": "2026-01-01",
        "end_date": "2026-12-31",
        "food_relation": "Before Bed",
        "notes": "Cardiovascular maintenance."
    }
]

medicines_store = list(default_medicines)

history_logs = [
    {
        "id": "log_001",
        "medicine_id": "med_001",
        "medicine_name": "Paracetamol",
        "dosage": "500 mg tablet",
        "scheduled_time": "08:00 AM",
        "status": "TAKEN",
        "timestamp": "Today at 08:05 AM",
        "notes": "Taken after breakfast"
    }
]

def add_cors_headers(response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Methods'] = 'GET, POST, PUT, DELETE, OPTIONS'
    response.headers['Access-Control-Allow-Headers'] = 'Content-Type, Authorization'
    return response

@app.after_request
def after_request_func(response):
    return add_cors_headers(response)

@app.route('/api/health', methods=['GET', 'OPTIONS'])
def health():
    if request.method == 'OPTIONS':
        return make_response('', 204)
    return jsonify({
        "status": "healthy",
        "service": "MediVoice AI API",
        "version": "1.0.0",
        "timestamp": datetime.datetime.utcnow().isoformat() + "Z"
    })

@app.route('/api/auth/login', methods=['POST', 'OPTIONS'])
def login():
    if request.method == 'OPTIONS':
        return make_response('', 204)
    data = request.get_json(silent=True) or {}
    email = data.get('email', '').strip().lower()
    password = data.get('password', '')

    user = next((u for u in users if u['email'].lower() == email and u['password'] == password), None)
    if user:
        return jsonify({
            "success": True,
            "user": {
                "id": user['id'],
                "name": user['name'],
                "email": user['email'],
                "phone": user.get('phone', '')
            },
            "token": "token_" + str(uuid.uuid4())
        })
    return jsonify({"success": False, "error": "Invalid email or password"}), 401

@app.route('/api/auth/register', methods=['POST', 'OPTIONS'])
def register():
    if request.method == 'OPTIONS':
        return make_response('', 204)
    data = request.get_json(silent=True) or {}
    name = data.get('name', '').strip()
    email = data.get('email', '').strip().lower()
    password = data.get('password', '')
    phone = data.get('phone', '').strip()

    if not name or not email or not password:
        return jsonify({"success": False, "error": "Name, email and password are required"}), 400

    if any(u['email'].lower() == email for u in users):
        return jsonify({"success": False, "error": "An account with this email already exists"}), 409

    new_user = {
        "id": "usr_" + str(uuid.uuid4())[:8],
        "name": name,
        "email": email,
        "phone": phone,
        "password": password
    }
    users.append(new_user)
    return jsonify({
        "success": True,
        "user": {
            "id": new_user['id'],
            "name": new_user['name'],
            "email": new_user['email'],
            "phone": new_user['phone']
        },
        "token": "token_" + str(uuid.uuid4())
    }), 201

@app.route('/api/medicines', methods=['GET', 'POST', 'OPTIONS'])
def medicines():
    if request.method == 'OPTIONS':
        return make_response('', 204)
    if request.method == 'GET':
        return jsonify({
            "success": True,
            "medicines": medicines_store
        })
    elif request.method == 'POST':
        data = request.get_json(silent=True) or {}
        name = data.get('name', '').strip()
        if not name:
            return jsonify({"success": False, "error": "Medicine name is required"}), 400
        
        new_med = {
            "id": "med_" + str(uuid.uuid4())[:8],
            "user_id": data.get("user_id", "usr_default_001"),
            "name": name,
            "type": data.get("type", "Tablet"),
            "dosage_name": data.get("dosage_name", "Standard Dose"),
            "dosage_amount": data.get("dosage_amount", 1),
            "dosage_unit": data.get("dosage_unit", "dose"),
            "daily_dosage": data.get("daily_dosage", "1 tablet"),
            "dosage_limit": data.get("dosage_limit", "As directed"),
            "times_per_day": int(data.get("times_per_day", 1)),
            "reminder_times": data.get("reminder_times", ["08:00 AM"]),
            "start_date": data.get("start_date", datetime.date.today().isoformat()),
            "end_date": data.get("end_date", ""),
            "food_relation": data.get("food_relation", "After Food"),
            "notes": data.get("notes", "")
        }
        medicines_store.append(new_med)
        return jsonify({"success": True, "medicine": new_med}), 201

@app.route('/api/medicines/<med_id>', methods=['DELETE', 'OPTIONS'])
def delete_medicine(med_id):
    if request.method == 'OPTIONS':
        return make_response('', 204)
    global medicines_store
    medicines_store = [m for m in medicines_store if m['id'] != med_id]
    return jsonify({"success": True, "message": "Medicine removed successfully"})

@app.route('/api/history', methods=['GET', 'POST', 'OPTIONS'])
def history():
    if request.method == 'OPTIONS':
        return make_response('', 204)
    if request.method == 'GET':
        return jsonify({"success": True, "history": history_logs})
    elif request.method == 'POST':
        data = request.get_json(silent=True) or {}
        new_log = {
            "id": "log_" + str(uuid.uuid4())[:8],
            "medicine_id": data.get("medicine_id", ""),
            "medicine_name": data.get("medicine_name", "Medicine"),
            "dosage": data.get("dosage", "1 dose"),
            "scheduled_time": data.get("scheduled_time", "Scheduled"),
            "status": data.get("status", "TAKEN"),
            "timestamp": datetime.datetime.now().strftime("%I:%M %p"),
            "notes": data.get("notes", "")
        }
        history_logs.insert(0, new_log)
        return jsonify({"success": True, "log": new_log}), 201

@app.route('/api/voice-assistant', methods=['POST', 'OPTIONS'])
def voice_assistant():
    if request.method == 'OPTIONS':
        return make_response('', 204)
    data = request.get_json(silent=True) or {}
    query = data.get('query', '').strip().lower()

    if not query:
        return jsonify({
            "speech": "I'm listening. You can ask what medicines you have today, or tell me to log a medicine.",
            "action": "none"
        })

    # Voice Intent Processing
    if any(q in query for q in ["what medicine", "today's medicine", "my schedule", "what do i take", "schedule"]):
        med_names = [m['name'] + " (" + ", ".join(m['reminder_times']) + ")" for m in medicines_store]
        speech = f"You have {len(medicines_store)} medicines scheduled today: " + "; ".join(med_names) + "."
        return jsonify({"speech": speech, "action": "show_schedule", "count": len(medicines_store)})

    if any(q in query for q in ["took", "taken", "mark", "i drank", "i had"]):
        for med in medicines_store:
            if med['name'].lower() in query:
                # Add to history
                new_log = {
                    "id": "log_" + str(uuid.uuid4())[:8],
                    "medicine_id": med['id'],
                    "medicine_name": med['name'],
                    "dosage": med['dosage_name'],
                    "scheduled_time": "Now",
                    "status": "TAKEN",
                    "timestamp": datetime.datetime.now().strftime("%I:%M %p"),
                    "notes": "Logged via Voice Assistant"
                }
                history_logs.insert(0, new_log)
                speech = f"Great job! I have recorded your dose of {med['name']} as taken."
                return jsonify({"speech": speech, "action": "mark_taken", "medicine": med['name']})
        speech = "I noted your request, but couldn't match the specific medicine name. Please mention the medicine name."
        return jsonify({"speech": speech, "action": "unmatched"})

    if "next" in query or "upcoming" in query:
        speech = "Your next scheduled dose is Paracetamol 500 mg at 8:00 PM."
        return jsonify({"speech": speech, "action": "show_next"})

    speech = f"I heard '{query}'. MediVoice AI can help check your doses, mark medicines taken, or check your schedule."
    return jsonify({"speech": speech, "action": "general"})

# Local development runner
if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port, debug=True)
