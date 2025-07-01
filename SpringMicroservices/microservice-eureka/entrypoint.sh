#!/bin/sh

echo "⏳ Esperando a config-server en http://config-server:8888/actuator/health ..."
until curl -s http://config-server:8888/actuator/health | grep '"status":"UP"' > /dev/null
do
  echo "⏱️  Aún no disponible, reintentando..."
  sleep 5
done

echo "✅ Config Server listo. Iniciando Eureka..."
exec java -jar app.jar
