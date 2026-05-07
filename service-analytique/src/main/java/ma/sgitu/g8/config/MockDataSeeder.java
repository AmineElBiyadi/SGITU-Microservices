package ma.sgitu.g8.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.StatSnapshotRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("dev")
@Component
public class MockDataSeeder implements CommandLineRunner {

    private final StatSnapshotRepository snapshotRepository;

    public MockDataSeeder(StatSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (snapshotRepository.count() == 0) {
            String mockJson = "[\n" +
                    "  {\n" +
                    "    \"id\": \"snap001\",\n" +
                    "    \"snapshotType\": \"TRIPS\",\n" +
                    "    \"period\": \"2026-05-01\",\n" +
                    "    \"metadata\": {\n" +
                    "      \"totalValidated\": 2,\n" +
                    "      \"totalExpired\": 1,\n" +
                    "      \"byLine\": {\n" +
                    "        \"L1\": 2,\n" +
                    "        \"L2\": 1\n" +
                    "      },\n" +
                    "      \"peakHour\": \"10:00-11:00\",\n" +
                    "      \"averageOccupancyRate\": 0.73\n" +
                    "    },\n" +
                    "    \"computedAt\": \"2026-05-01T12:00:00Z\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"id\": \"snap002\",\n" +
                    "    \"snapshotType\": \"REVENUE\",\n" +
                    "    \"period\": \"2026-05-01\",\n" +
                    "    \"metadata\": {\n" +
                    "      \"totalRevenue\": 250,\n" +
                    "      \"byType\": {\n" +
                    "        \"ticket\": 50,\n" +
                    "        \"subscription\": 200\n" +
                    "      },\n" +
                    "      \"failedPayments\": 1,\n" +
                    "      \"failedAmount\": 20\n" +
                    "    },\n" +
                    "    \"computedAt\": \"2026-05-01T15:00:00Z\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"id\": \"snap003\",\n" +
                    "    \"snapshotType\": \"INCIDENTS\",\n" +
                    "    \"period\": \"2026-05-01\",\n" +
                    "    \"metadata\": {\n" +
                    "      \"totalOpen\": 3,\n" +
                    "      \"totalResolved\": 0,\n" +
                    "      \"byZone\": {\n" +
                    "        \"Z1\": 3,\n" +
                    "        \"Z2\": 0\n" +
                    "      },\n" +
                    "      \"byType\": {\n" +
                    "        \"delay\": 2,\n" +
                    "        \"breakdown\": 1\n" +
                    "      },\n" +
                    "      \"criticalZone\": \"Z1\"\n" +
                    "    },\n" +
                    "    \"computedAt\": \"2026-05-01T10:00:00Z\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"id\": \"snap004\",\n" +
                    "    \"snapshotType\": \"VEHICLES\",\n" +
                    "    \"period\": \"2026-05-01\",\n" +
                    "    \"metadata\": {\n" +
                    "      \"totalActive\": 1,\n" +
                    "      \"totalInactive\": 1,\n" +
                    "      \"utilizationRate\": 0.50,\n" +
                    "      \"byLine\": {\n" +
                    "        \"L1\": \"in_service\",\n" +
                    "        \"L2\": \"out_of_service\"\n" +
                    "      }\n" +
                    "    },\n" +
                    "    \"computedAt\": \"2026-05-01T14:10:00Z\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"id\": \"snap005\",\n" +
                    "    \"snapshotType\": \"USERS\",\n" +
                    "    \"period\": \"2026-05-01\",\n" +
                    "    \"metadata\": {\n" +
                    "      \"totalActive\": 1,\n" +
                    "      \"totalInactive\": 1,\n" +
                    "      \"churnRate\": 0.50\n" +
                    "    },\n" +
                    "    \"computedAt\": \"2026-05-01T13:10:00Z\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"id\": \"snap006\",\n" +
                    "    \"snapshotType\": \"SUBSCRIPTIONS\",\n" +
                    "    \"period\": \"2026-05-01\",\n" +
                    "    \"metadata\": {\n" +
                    "      \"totalCreated\": 1,\n" +
                    "      \"totalRenewed\": 1,\n" +
                    "      \"totalCancelled\": 0,\n" +
                    "      \"byType\": {\n" +
                    "        \"monthly\": 1,\n" +
                    "        \"yearly\": 1\n" +
                    "      },\n" +
                    "      \"mostPopular\": \"yearly\",\n" +
                    "      \"totalRevenue\": 2000\n" +
                    "    },\n" +
                    "    \"computedAt\": \"2026-05-01T12:30:00Z\"\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"id\": \"snap007\",\n" +
                    "    \"snapshotType\": \"DASHBOARD\",\n" +
                    "    \"period\": \"2026-05-01\",\n" +
                    "    \"metadata\": {\n" +
                    "      \"totalTicketsValidated\": 2,\n" +
                    "      \"totalRevenue\": 250,\n" +
                    "      \"totalOpenIncidents\": 3,\n" +
                    "      \"activeVehicles\": 1,\n" +
                    "      \"inactiveVehicles\": 1,\n" +
                    "      \"criticalZone\": \"Z1\",\n" +
                    "      \"activeUsers\": 1\n" +
                    "    },\n" +
                    "    \"computedAt\": \"2026-05-01T15:00:00Z\"\n" +
                    "  }\n" +
                    "]";

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            List<StatSnapshot> snapshots = mapper.readValue(mockJson, new TypeReference<List<StatSnapshot>>() {});
            snapshotRepository.saveAll(snapshots);
            System.out.println("Mock StatSnapshots seeded!");
        }
    }
}
