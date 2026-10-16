package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
}

type Client struct {
	ID     string          `json:"deviceId"`
	Role   string          `json:"role"`
	Name   string          `json:"deviceName"`
	Socket *websocket.Conn `json:"-"`
}

type HostInfo struct {
	DeviceID   string `json:"deviceId"`
	DeviceName string `json:"deviceName"`
	Online     bool   `json:"online"`
}

type Message struct {
	Type string          `json:"type"`
	Data json.RawMessage `json:"-"`
}

type Hub struct {
	mu      sync.Mutex
	clients map[string]*Client
}

var hub = Hub{
	clients: make(map[string]*Client),
}

func main() {
	// Render يمرر المنفذ عبر متغير PORT، وإذا لم يوجد نستخدم 10000 افتراضياً
	port := os.Getenv("PORT")
	if port == "" {
		port = "10000"
	}

	// مسار الفحص الصحي الأساسي لـ Render
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("RemoteBridge Signaling Server is Live!"))
	})

	http.HandleFunc("/ws", handleWebSocket)

	// الاستماع الإلزامي على 0.0.0.0 لجميع واجهات الحاوية
	addr := "0.0.0.0:" + port
	log.Printf("Signaling server successfully bound and listening on %s", addr)
	if err := http.ListenAndServe(addr, nil); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}

func handleWebSocket(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("Upgrade error:", err)
		return
	}
	defer conn.Close()

	var currentDeviceID string

	defer func() {
		if currentDeviceID != "" {
			hub.mu.Lock()
			delete(hub.clients, currentDeviceID)
			hub.mu.Unlock()
			broadcastHostOffline(currentDeviceID)
		}
	}()

	for {
		_, msgBytes, err := conn.ReadMessage()
		if err != nil {
			break
		}

		var base map[string]interface{}
		if err := json.Unmarshal(msgBytes, &base); err != nil {
			continue
		}

		msgType, _ := base["type"].(string)

		switch msgType {
		case "register":
			var reg struct {
				DeviceID   string `json:"deviceId"`
				Role       string `json:"role"`
				DeviceName string `json:"deviceName"`
			}
			json.Unmarshal(msgBytes, &reg)

			currentDeviceID = reg.DeviceID
			client := &Client{
				ID:     reg.DeviceID,
				Role:   reg.Role,
				Name:   reg.Name,
				Socket: conn,
			}

			hub.mu.Lock()
			hub.clients[reg.DeviceID] = client
			hub.mu.Unlock()

			resp, _ := json.Marshal(map[string]interface{}{
				"type": "registered",
				"ok":   true,
			})
			conn.WriteMessage(websocket.TextMessage, resp)

			if reg.Role == "host" {
				broadcastHostOnline(reg.DeviceID, reg.Name)
			} else if reg.Role == "controller" {
				sendHostList(conn)
			}

		case "offer", "answer", "ice", "reject":
			var meta struct {
				To string `json:"to"`
			}
			json.Unmarshal(msgBytes, &meta)

			hub.mu.Lock()
			targetClient, ok := hub.clients[meta.To]
			hub.mu.Unlock()

			if ok && targetClient.Socket != nil {
				targetClient.Socket.WriteMessage(websocket.TextMessage, msgBytes)
			}

		case "ping":
			resp, _ := json.Marshal(map[string]interface{}{
				"type": "pong",
				"t":    time.Now().UnixMilli(),
			})
			conn.WriteMessage(websocket.TextMessage, resp)
		}
	}
}

func sendHostList(conn *websocket.Conn) {
	hub.mu.Lock()
	var hosts []HostInfo
	for _, c := range hub.clients {
		if c.Role == "host" {
			hosts = append(hosts, HostInfo{
				DeviceID:   c.ID,
				DeviceName: c.Name,
				Online:     true,
			})
		}
	}
	hub.mu.Unlock()

	msg, _ := json.Marshal(map[string]interface{}{
		"type":  "hosts",
		"hosts": hosts,
	})
	conn.WriteMessage(websocket.TextMessage, msg)
}

func broadcastHostOnline(deviceID, deviceName string) {
	hub.mu.Lock()
	defer hub.mu.Unlock()

	msg, _ := json.Marshal(map[string]interface{}{
		"type": "host_online",
		"host": HostInfo{
			DeviceID:   deviceID,
			DeviceName: deviceName,
			Online:     true,
		},
	})

	for _, c := range hub.clients {
		if c.Role == "controller" && c.Socket != nil {
			c.Socket.WriteMessage(websocket.TextMessage, msg)
		}
	}
}

func broadcastHostOffline(deviceID string) {
	hub.mu.Lock()
	defer hub.mu.Unlock()

	msg, _ := json.Marshal(map[string]interface{}{
		"type":     "host_offline",
		"deviceId": deviceID,
	})

	for _, c := range hub.clients {
		if c.Role == "controller" && c.Socket != nil {
			c.Socket.WriteMessage(websocket.TextMessage, msg)
		}
	}
}
