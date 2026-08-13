``` mermaid
classDiagram
    Equipment --> EquipType : type
    Equipment --> InspectionItem : inspections
    Equipment --> EquipStatus: status
    Equipment --> EquipmentSpec: spec
    Equipment ..> InspectionPolicy : uses

    EquipmentSpec <|.. ParticleSamplerSpec
    EquipmentSpec <|.. GasSamplerSpec
    EquipmentSpec <|.. GasAnalyzerSpec
    EquipmentSpec <|.. PitotTubeSpec
    EquipmentSpec <|.. NozzleSpec
    EquipmentSpec <|.. OtherSpec
    
    InspectionItem --> InspectionType : type
    
    InspectionPolicy ..> EquipType
    InspectionPolicy ..> InspectionItem

    class Equipment {
        -EquipType type           
        -List~InspectionItem~ inspections
        -EquipStatus stauts
        -EquipmentSpec spec
        
        +register(...) Equipment
    }

    note for EquipType "PARTICLE_SAMPLER: 입자상 시료채취장비<br>GAS_SAMPLER: 가스상 시료채취장비<br>GAS_ANALYZER: 배출가스 분석기<br>PITOT_TUBE: 피토우관<br>NOZZLE: 노즐<br>OTHER: 기타"
    class EquipType {
        <<enumeration>>
        PARTICLE_SAMPLER
        GAS_SAMPLER
        GAS_ANALYZER
        PITOT_TUBE
        NOZZLE
        OTHER
    }
    
    note for EquipStatus "ACTIVE: 사용가능<br>INACTIVE: 사용 중지<br>MAINTENANCE: 점검·수리·보정<br>DELETED: 장비 삭제"
    class EquipStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE,
        MAINTENANCE
        DELETED
    }
    
    class EquipmentSpec {
        <<interface>>
    }

    class ParticleSamplerSpec
    class GasSamplerSpec
    class GasAnalyzerSpec
    class PitotTubeSpec
    class NozzleSpec
    class OtherSpec
    
    class InspectionItem {
        -InspectionType type
        -boolean enabled
        -Interger cycleMonths
        -LocalDate lastInspectedAt
        -LocalDate nextDueDateOverride
        -boolean notificationEnabled
    }
    
    note for InspectionType "PRECISION_INSPECTION: 정도검사<br>CALIBRATION: 교정<br>GENERAL_TEST: 일반시험"
    class InspectionType {
        <<enumeration>>
        PRECISION_INSPECTION
        CALIBRATION
        GENERAL_TEST
    }
    
    class InspectionPolicy {
        <<Domain Policy>>
        -Map DEFAULTS$
        +defaultsFor(EquipType type)$ List~InspectionItem~
    }
```
