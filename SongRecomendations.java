
//Author: Richard Alonso Garcia
//Email: ralonsogarci2023@my.fit.edu


import java.io.*;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SongRecomendations{
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("input correct number of files");
            return;
        }

        String initialRatingsFile = args[0];
        String actionsFile = args[1];
        String targetCustomerName = null;
        List<Customer> customers = new ArrayList<>();

        // Read initial ratings file
        try (BufferedReader reader = new BufferedReader(new FileReader(initialRatingsFile))) {
            targetCustomerName = reader.readLine().trim();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] tokens = line.split("\\s+");
                String customerName = tokens[0];
                int[] ratings = new int[10];
                for (int i = 0; i < 10; i++) {
                    ratings[i] = Integer.parseInt(tokens[i + 1]);
                }
                Customer customer = new Customer(customerName, ratings);
                insertCustomer(customers, customer);
            }
        } catch (IOException e) {
            System.out.println("Error reading initial ratings file: " + e.getMessage());
            return;
        }

        // Read actions file
        try (BufferedReader actionReader = new BufferedReader(new FileReader(actionsFile))) {
            String actionLine;
            while ((actionLine = actionReader.readLine()) != null) {
                actionLine = actionLine.trim();
                if (actionLine.isEmpty()) continue;

                if (actionLine.startsWith("AddCustomer")) {
                    processAddCustomer(actionLine, customers);
                } else if (actionLine.equals("RecommendSongs")) {
                    processRecommendSongs(targetCustomerName, customers);
                } else if (actionLine.equals("PrintCustomerDistanceRatings")) {
                    processPrintCustomerDistanceRatings(targetCustomerName, customers);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading actions file: " + e.getMessage());
        }
    }

    private static void insertCustomer(List<Customer> customers, Customer customer) {
        int index = Collections.binarySearch(customers, customer);
        if (index < 0) {
            index = -index - 1;
        }
        customers.add(index, customer);
    }

    private static void processAddCustomer(String actionLine, List<Customer> customers) {
        String[] tokens = actionLine.split("\\s+");
        String customerName = tokens[1];
        int[] ratings = new int[10];
        for (int i = 0; i < 10; i++) {
            ratings[i] = Integer.parseInt(tokens[i + 2]);
        }
        Customer customer = new Customer(customerName, ratings);
        insertCustomer(customers, customer);
        System.out.println(actionLine);
    }

    private static void processRecommendSongs(String targetCustomerName, List<Customer> customers) {
        Customer targetCustomer = findCustomerByName(customers, targetCustomerName);
        if (targetCustomer == null) {
            System.out.println("RecommendSongs none");
            return;
        }

        Comparator<CustomerDistance> comparator = (cd1, cd2) -> {
            int cmp = Double.compare(cd1.distance, cd2.distance);
            return (cmp != 0) ? cmp : cd1.customer.name.compareTo(cd2.customer.name);
        };
        PriorityQueue<CustomerDistance> pq = new PriorityQueue<>(comparator);

        for (Customer other : customers) {
            if (!other.name.equals(targetCustomer.name)) {
                double distance = calculateDistance(targetCustomer, other);
                if (distance >= 0) {
                    pq.offer(new CustomerDistance(other, distance));
                }
            }
        }

        while (!pq.isEmpty()) {
            CustomerDistance closest = pq.poll();
            List<Integer> recommendedSongs = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                if (closest.customer.ratings[i] >= 4 && targetCustomer.ratings[i] == 0) {
                    recommendedSongs.add(i);
                }
            }

            if (!recommendedSongs.isEmpty()) {
                System.out.print("RecommendSongs " + closest.customer.name);
                for (int index : recommendedSongs) {
                    System.out.print(" song" + (index + 1) + " " + closest.customer.ratings[index]);
                }
                System.out.println();
                return;
            }
        }

        System.out.println("RecommendSongs none");
    }

    private static void processPrintCustomerDistanceRatings(String targetCustomerName, List<Customer> customers) {
        System.out.println("PrintCustomerDistanceRatings");
        Customer targetCustomer = findCustomerByName(customers, targetCustomerName);
        if (targetCustomer == null) {
            System.out.println("Target customer not found.");
            return;
        }

        printCustomerLine("-----", targetCustomer);
        for (Customer customer : customers) {
            if (!customer.name.equals(targetCustomer.name)) {
                double distance = calculateDistance(targetCustomer, customer);
                String distStr = (distance >= 0) ? String.format("%.3f", distance) : "-----";
                printCustomerLine(distStr, customer);
            }
        }
    }

    private static void printCustomerLine(String dist, Customer customer) {
        System.out.printf("%-6s %-10s", dist, customer.name);
        for (int r : customer.ratings) {
            System.out.printf(" %d", r);
        }
        System.out.println();
    }

    public static double calculateDistance(Customer x, Customer y) {
        List<Integer> shared = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (x.ratings[i] > 0 && y.ratings[i] > 0) {
                shared.add(i);
            }
        }
        if (shared.isEmpty()) return -1.0;

        double diffSum = 0.0;
        for (int i : shared) {
            diffSum += Math.abs(x.ratings[i] - y.ratings[i]);
        }

        BigDecimal diffBD = BigDecimal.valueOf(diffSum);
        BigDecimal sizeBD = BigDecimal.valueOf(shared.size());
        BigDecimal avgDiff = diffBD.divide(sizeBD, 10, RoundingMode.HALF_UP);
        BigDecimal reciprocal = BigDecimal.ONE.divide(sizeBD, 10, RoundingMode.HALF_UP);
        BigDecimal total = reciprocal.add(avgDiff).setScale(3, RoundingMode.HALF_UP);

        return total.doubleValue();
    }

    private static Customer findCustomerByName(List<Customer> customers, String name) {
        int index = Collections.binarySearch(customers, new Customer(name, null));
        return index >= 0 ? customers.get(index) : null;
    }

    static class Customer implements Comparable<Customer> {
        String name;
        int[] ratings;

        public Customer(String name, int[] ratings) {
            this.name = name;
            this.ratings = ratings;
        }

        @Override
        public int compareTo(Customer other) {
            return this.name.compareTo(other.name);
        }
    }

    static class CustomerDistance {
        Customer customer;
        double distance;

        public CustomerDistance(Customer customer, double distance) {
            this.customer = customer;
            this.distance = distance;
        }
    }
}
