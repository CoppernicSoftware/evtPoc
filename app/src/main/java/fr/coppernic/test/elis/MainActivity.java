package fr.coppernic.test.elis;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Collection;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.coppernic.sdk.barcode.BarcodeFactory;
import fr.coppernic.sdk.barcode.BarcodeReader;
import fr.coppernic.sdk.barcode.BarcodeReader.BarcodeListener;
import fr.coppernic.sdk.barcode.BarcodeReaderType;
import fr.coppernic.sdk.barcode.SymbolSetting;
import fr.coppernic.sdk.barcode.core.Parameter;
import fr.coppernic.sdk.powermgmt.PowerMgmt;
import fr.coppernic.sdk.powermgmt.PowerMgmtFactory;
import fr.coppernic.sdk.powermgmt.cone.identifiers.InterfacesCone;
import fr.coppernic.sdk.powermgmt.cone.identifiers.ManufacturersCone;
import fr.coppernic.sdk.powermgmt.cone.identifiers.ModelsCone;
import fr.coppernic.sdk.powermgmt.cone.identifiers.PeripheralTypesCone;
import fr.coppernic.sdk.utils.core.CpcResult;
import fr.coppernic.sdk.utils.debug.L;
import fr.coppernic.sdk.utils.io.InstanceListener;
import fr.coppernic.sdk.utils.ui.UiHandler;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "Evt";
    private final static boolean DEBUG = true;
    private PowerMgmt powerMgmt;
    private BarcodeReader reader;
    private long counter = 0;
    private long average = 0;
    private long startTime = 0;
    private long endTime = 0;
    private boolean isScanning = false;
    private final BarcodeListener barcodeListener = new BarcodeListener(){

        @Override
        public void onOpened(CpcResult.RESULT result) {
            scan();
        }

        @Override
        public void onFirmware(CpcResult.RESULT result, String s) {

        }

        @Override
        public void onScan(CpcResult.RESULT result, BarcodeReader.ScanResult scanResult) {
            endTime = System.currentTimeMillis();
            L.m(TAG, DEBUG, result.toString() + ", " + (scanResult==null?"null":scanResult));
            if(result == CpcResult.RESULT.CANCELLED){
                isScanning = false;
            } else {
                if (scanResult != null) {
                    ++counter;
                    long time = endTime - startTime;
                    Log.d(TAG, "time " + time);
                    average = ((average * (counter - 1)) + time) / counter;
                    tvData.setText(scanResult.dataToString());
                    tvCounter.setText("Counter : " + counter);
                    tvAverage.setText(average + " ms");
                }
                scan();
            }

        }

        @Override
        public void onParameterAvailable(CpcResult.RESULT result, Parameter parameter) {

        }

        @Override
        public void onSymbolSettingAvailable(CpcResult.RESULT result, SymbolSetting symbolSetting) {

        }

        @Override
        public void onAllSymbolSettingsAvailable(CpcResult.RESULT result, Collection<SymbolSetting> collection) {

        }

        @Override
        public void onSettingsSaved(CpcResult.RESULT result) {

        }
    };


    @BindView(R.id.data)
    TextView tvData;
    @BindView(R.id.counter)
    TextView tvCounter;
    @BindView(R.id.average)
    TextView tvAverage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        powerMgmt = getPowerMgmt();
        powerMgmt.powerOn();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isScanning){
                    reader.abortScan();
                    reader.close();
                    powerMgmt.powerOff();
                    isScanning = false;
                } else {
                    powerMgmt.powerOn();
                    reader.open();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        BarcodeReader.ServiceManager.stopService(this);
        getBarcodeReader();
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        reader.abortScan();
        reader.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void scan(){
        isScanning = true;
        startTime = System.currentTimeMillis();
        reader.scan();
    }

    private PowerMgmt getPowerMgmt(){
        PowerMgmtFactory factory = PowerMgmtFactory.get()
            .setContext(this)
            // 500ms is neede by the reader to initialize
            .setTimeToSleepAfterPowerOn(1000)
            .setTimeToSleepAfterPowerOff(100);

// In this example we are telling Powermgmt to use Barcore reader Opticon Mdi3100 that is installed on C-One
        factory.setPeripheralTypes(PeripheralTypesCone.BarcodeReader);
        factory.setInterfaces(InterfacesCone.ScannerPort);
        factory.setManufacturers(ManufacturersCone.Opticon);
        factory.setModels(ModelsCone.Mdi3100);

        return factory.build();
    }

    private void getBarcodeReader(){
        BarcodeFactory factory = BarcodeFactory.get()
            .setBarcodeListener(barcodeListener)
            // C-One are shipped with reader working at 115200
            .setBaudrate(115200)
            //Port should be good by default
            //Type should be good by default also
            .setType(BarcodeReaderType.OPTICON_MDI3100)
            .setAppHandler(new UiHandler());

        factory.build(this, new InstanceListener<BarcodeReader>() {
            @Override
            public void onCreated(BarcodeReader barcodeReader) {
                reader = barcodeReader;
                reader.open();
            }

            @Override
            public void onDisposed(BarcodeReader barcodeReader) {

            }
        });
    }

}
