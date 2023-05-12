package com.example.fingerprintapp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import com.machinezoo.sourceafis.FingerprintImage;
import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;

public class App {

    private static final int  HOSTS = 1;
    private static final int  HOST_PES = 8;
    private static final int  HOST_MIPS = 1000; // Milion Instructions per Second (MIPS)
    private static final int  HOST_RAM = 2048; //in Megabytes
    private static final long HOST_BW = 10_000; //in Megabits/s
    private static final long HOST_STORAGE = 1_000_000; //in Megabytes

    private static final int VMS = 2;
    private static final int VM_PES = 4;

    private static final int CLOUDLETS = 4;
    private static final int CLOUDLET_PES = 2;
    private static final int CLOUDLET_LENGTH = 10_000; // Milion Instructions (MI)

    private final CloudSimPlus simulation;
    private final DatacenterBroker broker0;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private Datacenter datacenter0;

    public static void main(String[] args) throws IOException {
        new App();
    }

    private App() throws IOException {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);

        simulation = new CloudSimPlus();
        datacenter0 = createDatacenter();

        //Creates a broker that is a software acting on behalf of a cloud customer to manage his/her VMs and Cloudlets
        broker0 = new DatacenterBrokerSimple(simulation);

        vmList = createVms();
        cloudletList = createCloudlets();
        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);

        simulation.start();

        // add fingerprints to cloudlet
        addFingerprintsToCloudlet();

        // read fingerprint path from user
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter fingerprint path: ");
        String fingerprintPath = sc.nextLine();
        sc.close();

        // read fingerprint image in byte array
        byte[] fingerprintImage = Files.readAllBytes(new File(fingerprintPath).toPath());

        // create fingerprint image object
        FingerprintImage fingerprint = new FingerprintImage(fingerprintImage);

        // create candidate template from fingerprint image
        FingerprintTemplate candidate = new FingerprintTemplate(fingerprint);

        // get files from cloudlet
        List<String> files = cloudletList.get(0).getRequiredFiles();

        // compare fingerprint with each file
        for (String file : files) {
            // read fingerprint template from file
            byte[] fingerprintTemplate = Files.readAllBytes(new File(file).toPath());

            // create fingerprint template object
            FingerprintTemplate template = new FingerprintTemplate(fingerprintTemplate);

            // compare fingerprint with template
            FingerprintMatcher matcher = new FingerprintMatcher(template);
            double score = matcher.match(candidate);

            // if score is greater than 40 then fingerprint is matched
            if (score > 40) {
                System.out.println("Fingerprint matched with " + file);
            }
        }


        final List<? extends Cloudlet> cloudletFinishedList = broker0.getCloudletFinishedList();
        new CloudletsTableBuilder(cloudletFinishedList).build();
    }

    /**
     * Creates a Datacenter and its Hosts.
     */
    private Datacenter createDatacenter() {
        final List<Host> hostList = new ArrayList<Host>(HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            final Host host = createHost();
            hostList.add(host);
        }

        //Uses a VmAllocationPolicySimple by default to allocate VMs
        return new DatacenterSimple(simulation, hostList);
    }

    private Host createHost() {
        final List<Pe> peList = new ArrayList<Pe>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            peList.add(new PeSimple(HOST_MIPS));
        }

        /*
        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
        and VmSchedulerSpaceShared for VM scheduling.
        */
        return new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
    }

    /**
     * Creates a list of VMs.
     */
    private List<Vm> createVms() {
        final List<Vm> vmList = new ArrayList<Vm>(VMS);
        for (int i = 0; i < VMS; i++) {
            //Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
            final VmSimple vm = new VmSimple(HOST_MIPS, VM_PES);
            vm.setRam(512).setBw(1000).setSize(10_000);
            vmList.add(vm);
        }

        return vmList;
    }

    /**
     * Creates a list of Cloudlets.
     */
    private List<Cloudlet> createCloudlets() {
        final List<Cloudlet> cloudletList = new ArrayList<Cloudlet>(CLOUDLETS);

        //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.5);

        for (int i = 0; i < CLOUDLETS; i++) {
            final CloudletSimple cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES, utilizationModel);
            cloudlet.setSizes(1024);
            cloudletList.add(cloudlet);
        }

        return cloudletList;
    }

    /**
     * Add the dataset of fingerprints to the cloudlet
     * @throws IOException
     */
    private void addFingerprintsToCloudlet() throws IOException {
        for (int i = 0; i < CLOUDLETS; i++) {
            // get cloudlet
            Cloudlet cloudlet = cloudletList.get(i);

            String path = "path to database";

            // add fingerprint to cloudlet
            cloudlet.addRequiredFile(path);
        }
    }

    /**
     * get fingerprint from cloudlet
     */
    private List<String> getFingerprintFromCloudlet(int cloudletId) {
        // get cloudlet
        Cloudlet cloudlet = cloudletList.get(cloudletId);

        // get all files from cloudlet
        List<String> files = cloudlet.getRequiredFiles();

        return files;

    }

}
